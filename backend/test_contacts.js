const http = require('http');

const baseURL = 'http://localhost:3000/api/contactos';
const usuario_id = 1; // Assuming user with id 1 exists

async function request(options, body = null) {
    return new Promise((resolve, reject) => {
        const req = http.request(options, (res) => {
            let data = '';
            res.on('data', (chunk) => data += chunk);
            res.on('end', () => {
                try {
                    resolve({
                        status: res.statusCode,
                        body: data ? JSON.parse(data) : null
                    });
                } catch (e) {
                    resolve({ status: res.statusCode, body: data });
                }
            });
        });
        req.on('error', reject);
        if (body) req.write(JSON.stringify(body));
        req.end();
    });
}

async function runTests() {
    console.log('--- Testing Create Contact ---');
    const createRes = await request({
        method: 'POST',
        hostname: 'localhost',
        port: 3000,
        path: '/api/contactos',
        headers: { 'Content-Type': 'application/json' }
    }, {
        usuario_id,
        nombre: 'Juan Perez',
        cuenta_bancaria: '12345678',
        nombre_banco: 'Banco Union',
        alias: 'Juancito'
    });
    console.log('Create Response:', createRes);
    if (createRes.status !== 201) process.exit(1);
    const contactoId = createRes.body.contacto.id;

    console.log('\n--- Testing Get Contacts ---');
    const getRes = await request({
        method: 'GET',
        hostname: 'localhost',
        port: 3000,
        path: `/api/contactos/usuario/${usuario_id}`
    });
    console.log('Get Response:', getRes);
    if (getRes.status !== 200) process.exit(1);

    console.log('\n--- Testing Update Contact ---');
    const updateRes = await request({
        method: 'PUT',
        hostname: 'localhost',
        port: 3000,
        path: `/api/contactos/${contactoId}`,
        headers: { 'Content-Type': 'application/json' }
    }, {
        alias: 'Juan El Grande'
    });
    console.log('Update Response:', updateRes);
    if (updateRes.status !== 200) process.exit(1);

    console.log('\n--- Testing Delete Contact ---');
    const deleteRes = await request({
        method: 'DELETE',
        hostname: 'localhost',
        port: 3000,
        path: `/api/contactos/${contactoId}`
    });
    console.log('Delete Response:', deleteRes);
    if (deleteRes.status !== 200) process.exit(1);

    console.log('\n--- All tests passed! ---');
}

runTests().catch(console.error);
