const state = {
    token: localStorage.token, user: JSON.parse(localStorage.user || 'null')
};
const $ = s => document.querySelector(s);
const api = async (path, opt = {}) => {
    let r = await fetch('/api' + path, { ...opt, headers: { 'Content-Type': 'application/json', ...(state.token ? { Authorization: 'Bearer ' + state.token } : {}), ...(opt.headers || {}) } });
    let data = await r.json().catch(() => ({}));
    if (!r.ok) throw Error(data.error || 'Request failed');
    return data;
};
// Escapes text for HTML bodies and attribute values (filenames are user supplied)
function escape(v) { return String(v || '').replace(/[&<>"']/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[m])) }

function showApp() {
    $('#auth').hidden = true; $('#app').hidden = false;
    $('#user').textContent = `${state.user.name} · ${state.user.role}`;
    $('#logout').hidden = false;
    loadCitizen();
    if (state.user.role === 'OFFICIAL' || state.user.role === 'ADMIN') loadOfficial();
    if (state.user.role === 'ADMIN') loadAdmin();
}
function signIn(r) {
    state.token = r.token; state.user = { name: r.name, role: r.role };
    localStorage.token = r.token; localStorage.user = JSON.stringify(state.user);
    showApp();
}
function card(c, official = false, admin = false) {
    return `<article class="card"><h3>${escape(c.title)}</h3><div class="meta">${escape(c.category)} · ${escape(c.location)}${c.ward ? ' · Ward ' + escape(c.ward) : ''}</div><span class="status">${c.status}</span>${c.overdue ? '<div class="overdue">SLA overdue</div>' : ''}<div class="meta">Deadline: ${new Date(c.slaDeadline).toLocaleString()}</div><button onclick="details('${c.id}')">View details</button>${(official || admin) && c.status !== 'VERIFIED' ? ` <button onclick="advance('${c.id}','${c.status}')">Update status</button>` : ''}${admin && c.assignmentStatus !== 'AUTO_ASSIGNED' ? ` <button onclick="assign('${c.id}')">Assign</button>` : ''}</article>`;
}
async function loadCitizen() {
    let [items, notes] = await Promise.all([api('/complaints'), api('/complaints/notifications')]);
    $('#my-list').innerHTML = items.map(x => card(x)).join('') || '<p>No reports yet.</p>';
    $('#notifications').innerHTML = notes.slice(0, 5).map(n => `<p>${escape(n.message)}</p>`).join('') || '<p>No notifications.</p>';
}
async function loadOfficial() { try { $('#official-list').innerHTML = (await api('/official/complaints')).map(x => card(x, true)).join('') || '<p>No assigned complaints.</p>' } catch (e) { } }
async function loadAdmin() { try { $('#admin-list').innerHTML = (await api('/admin/complaints')).map(x => card(x, true, true)).join('') } catch (e) { } }

// ---------------------------------------------------------------------------
// Evidence (photos / videos): browser -> our API for a presigned URL -> browser -> S3 directly
// ---------------------------------------------------------------------------
const MAX_FILES = 5, MAX_IMAGE = 10 * 1024 * 1024, MAX_VIDEO = 50 * 1024 * 1024;
const FILE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'video/mp4', 'video/webm', 'video/quicktime'];
const FILE_ACCEPT = FILE_TYPES.join(',');

// Returns a problem description, or null when the file is acceptable
function fileProblem(f) {
    if (!FILE_TYPES.includes(f.type)) return 'unsupported file type';
    const max = f.type.startsWith('video/') ? MAX_VIDEO : MAX_IMAGE;
    return f.size > max ? 'too large (max ' + max / 1048576 + ' MB)' : null;
}
async function uploadOne(complaintId, file) {
    const ticket = await api(`/complaints/${complaintId}/media/presign`, { method: 'POST', body: JSON.stringify({ fileName: file.name, contentType: file.type, sizeBytes: file.size }) });
    // Straight to S3: no Authorization header here, the signature in the URL is the permission
    const put = await fetch(ticket.uploadUrl, { method: 'PUT', headers: ticket.headers, body: file });
    if (!put.ok) throw Error('storage rejected the upload (HTTP ' + put.status + ')');
    return api(`/complaints/${complaintId}/media/${ticket.mediaId}/confirm`, { method: 'POST' });
}
// Uploads files one by one; returns the list of failure messages
async function uploadAll(complaintId, files, say) {
    const failures = [];
    for (const [i, f] of files.entries()) {
        say(`Uploading ${i + 1} of ${files.length}: ${f.name}…`);
        const problem = fileProblem(f);
        try { if (problem) throw Error(problem); await uploadOne(complaintId, f); } catch (e) { failures.push(`${f.name} (${e.message})`); }
    }
    return failures;
}
function mediaItem(m) {
    const u = escape(m.url), n = escape(m.fileName);
    return m.contentType.startsWith('video/')
        ? `<figure><video controls preload="metadata" src="${u}"></video><figcaption>${n}</figcaption></figure>`
        : `<figure><a href="${u}" target="_blank" rel="noopener"><img loading="lazy" src="${u}" alt="${n}"></a><figcaption>${n}</figcaption></figure>`;
}
async function loadMedia(id) {
    const box = $('#media-box'); if (!box) return;
    try { box.innerHTML = (await api('/complaints/' + id + '/media')).map(mediaItem).join('') || '<p class="meta">No evidence attached.</p>' }
    catch (e) { box.innerHTML = '<p class="meta">Could not load evidence: ' + escape(e.message) + '</p>' }
}
window.addEvidence = async id => {
    const input = $('#more-files'), status = $('#more-status'), files = [...input.files];
    if (!files.length) return;
    if (files.length > MAX_FILES) { status.textContent = 'Choose at most ' + MAX_FILES + ' files.'; return }
    const failures = await uploadAll(id, files, m => status.textContent = m);
    status.textContent = failures.length ? 'Failed: ' + failures.join('; ') : 'Uploaded.';
    input.value = '';
    loadMedia(id);
};

window.details = async id => {
    let [c, h] = await Promise.all([api('/complaints/' + id), api('/complaints/' + id + '/history')]);
    const addForm = state.user.role === 'CITIZEN' ? `<p><input type="file" id="more-files" multiple accept="${FILE_ACCEPT}"> <button onclick="addEvidence('${id}')">Upload</button></p><p id="more-status" class="meta"></p>` : '';
    $('#detail-body').innerHTML = `<h2>${escape(c.title)}</h2><p>${escape(c.description)}</p><p><b>Assignment:</b> ${escape(c.assignmentStatus)}</p>${c.resolutionNotes ? `<p><b>Resolution:</b> ${escape(c.resolutionNotes)}</p>` : ''}<h3>Evidence</h3><div id="media-box" class="media-grid"><p class="meta">Loading…</p></div>${addForm}<h3>Timeline</h3>${h.map(x => `<div class="history"><b>${x.toStatus}</b><br>${escape(x.note || '')}<br><small>${new Date(x.createdAt).toLocaleString()}</small></div>`).join('')}${c.status === 'RESOLVED' ? '<button onclick="citizenDecision(\'' + id + '\',\'VERIFIED\')">Confirm resolution</button> <button onclick="citizenDecision(\'' + id + '\',\'REOPENED\')">Still unresolved</button>' : ''}`;
    $('#detail').showModal();
    loadMedia(id);
};
window.citizenDecision = async (id, status) => { let note = prompt(status === 'REOPENED' ? 'Why is this unresolved?' : 'Optional confirmation note') || ''; await api('/complaints/' + id + '/transition', { method: 'POST', body: JSON.stringify({ status, note }) }); $('#detail').close(); loadCitizen() };
window.advance = async (id, current) => { let next = { ASSIGNED: 'IN_PROGRESS', IN_PROGRESS: 'RESOLVED', REOPENED: 'IN_PROGRESS' }[current]; if (!next) return alert('This complaint cannot be progressed from its current state.'); let note = prompt(next === 'RESOLVED' ? 'Resolution notes (required in practice):' : 'Progress note') || ''; await api('/official/complaints/' + id + '/transition', { method: 'POST', body: JSON.stringify({ status: next, note }) }); loadOfficial(); loadAdmin() };
window.assign = async id => { let dept = prompt('Department UUID'); let official = prompt('Official UUID (optional)'); if (!dept) return; await api('/admin/complaints/' + id + '/assign', { method: 'POST', body: JSON.stringify({ departmentId: dept, officialId: official || null }) }); loadAdmin() };

$('#login').onsubmit = async e => {
    e.preventDefault();
    try { signIn(await api('/auth/login', { method: 'POST', body: JSON.stringify(Object.fromEntries(new FormData(e.target))) })) } catch (x) { $('#auth-error').textContent = x.message }
};
$('#register').onsubmit = async e => {
    e.preventDefault();
    try { signIn(await api('/auth/register', { method: 'POST', body: JSON.stringify(Object.fromEntries(new FormData(e.target))) })) } catch (x) { $('#auth-error').textContent = x.message }
};
$('#new-complaint').onsubmit = async e => {
    e.preventDefault();
    const form = e.target, status = $('#upload-status'), button = form.querySelector('button');
    const data = new FormData(form);
    const files = data.getAll('evidence').filter(f => f.size > 0); // an empty picker still sends one empty entry
    data.delete('evidence');
    if (files.length > MAX_FILES) { status.textContent = 'Choose at most ' + MAX_FILES + ' files.'; return }
    const bad = files.map(f => [f, fileProblem(f)]).filter(([, p]) => p);
    if (bad.length) { status.textContent = bad.map(([f, p]) => `${f.name}: ${p}`).join('; '); return }
    button.disabled = true; status.textContent = '';
    try {
        const complaint = await api('/complaints', { method: 'POST', body: JSON.stringify(Object.fromEntries(data)) });
        form.reset();
        // The complaint is saved first, so a failed upload never loses the report
        const failures = await uploadAll(complaint.id, files, m => status.textContent = m);
        status.textContent = failures.length ? 'Complaint submitted, but these files failed: ' + failures.join('; ') + '. Open the complaint to try again.' : (files.length ? 'Complaint and evidence submitted.' : '');
    } catch (x) { status.textContent = x.message }
    button.disabled = false;
    loadCitizen();
};
$('#new-department').onsubmit = async e => { e.preventDefault(); await api('/admin/departments', { method: 'POST', body: JSON.stringify(Object.fromEntries(new FormData(e.target))) }); e.target.reset(); loadAdmin() };
document.querySelectorAll('[data-view]').forEach(b => b.onclick = () => document.querySelectorAll('.view').forEach(v => v.hidden = v.id !== b.dataset.view));
$('#logout').onclick = () => { localStorage.clear(); location.reload() };
$('.close').onclick = () => $('#detail').close();
if (state.token && state.user) showApp();