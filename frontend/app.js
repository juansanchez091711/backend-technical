// Todas las llamadas pasan por el proxy /api de nginx hacia el backend.
const API = '/api';

// El backend no expone GET /products, así que se recuerdan los creados en esta sesión.
const products = [];

const $ = (id) => document.getElementById(id);

function log(method, url, status, body) {
    const line = `${new Date().toLocaleTimeString()}  ${method} ${url} → ${status}\n${JSON.stringify(body, null, 2)}\n\n`;
    $('log').textContent = line + $('log').textContent;
}

async function request(method, path, body) {
    const response = await fetch(API + path, {
        method,
        headers: body ? { 'Content-Type': 'application/json' } : {},
        body: body ? JSON.stringify(body) : undefined,
    });
    const data = await response.json().catch(() => null);
    log(method, path, response.status, data);
    if (!response.ok) {
        throw new Error(data?.message ?? `HTTP ${response.status}`);
    }
    return data;
}

function notify(error) {
    alert(`Error: ${error.message}`);
}

function refreshProductSelects() {
    for (const id of ['material-product', 'calc-product']) {
        const select = $(id);
        const current = select.value;
        select.innerHTML = '<option value="">Selecciona un producto</option>' +
            products.map(p => `<option value="${p.id}">${escapeHtml(p.name)} (#${p.id})</option>`).join('');
        select.value = current;
    }
    $('product-list').innerHTML = products
        .map(p => `<li>#${p.id} — ${escapeHtml(p.name)}</li>`).join('');
}

function fillTable(tableId, rows) {
    const table = $(tableId);
    table.querySelector('tbody').innerHTML = rows
        .map(cells => `<tr>${cells.map(c => `<td>${escapeHtml(String(c ?? ''))}</td>`).join('')}</tr>`).join('');
    table.hidden = false;
}

function escapeHtml(text) {
    return text.replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

$('product-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
        const product = await request('POST', '/products', { name: $('product-name').value });
        products.push(product);
        refreshProductSelects();
        e.target.reset();
    } catch (err) { notify(err); }
});

$('material-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const productId = $('material-product').value;
    try {
        const item = await request('POST', `/products/${productId}/materials`, {
            material: $('material-name').value,
            quantity: Number($('material-qty').value),
        });
        const product = products.find(p => String(p.id) === productId);
        $('material-list').insertAdjacentHTML('beforeend',
            `<li>${escapeHtml(product?.name ?? '#' + productId)}: ${escapeHtml(item.material)} × ${item.quantity}</li>`);
        $('material-name').value = '';
        $('material-qty').value = '';
    } catch (err) { notify(err); }
});

$('calc-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const params = new URLSearchParams({ productId: $('calc-product').value, quantity: $('calc-qty').value });
    try {
        const result = await request('GET', `/production/calculate?${params}`);
        fillTable('calc-table', result.materials.map(m => [m.material, m.required]));
    } catch (err) { notify(err); }
});

$('load-operators').addEventListener('click', async () => {
    try {
        const operators = await request('GET', '/operators');
        fillTable('operators-table', operators.map(o => [o.id, o.name, o.email, o.phone]));
    } catch (err) { notify(err); }
});
