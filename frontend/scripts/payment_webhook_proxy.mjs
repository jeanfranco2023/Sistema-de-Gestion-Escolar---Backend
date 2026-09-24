import http from 'node:http';

const webhookPath = '/api/v1/pagos/webhook/mercadopago';
const upstream = 'http://127.0.0.1:8081';
const maxBodyBytes = 64 * 1024;

http.createServer(async (request, response) => {
  const url = new URL(request.url ?? '/', 'http://localhost');
  if (url.pathname !== webhookPath || request.method !== 'POST') {
    response.writeHead(404).end();
    return;
  }

  try {
    const chunks = [];
    let total = 0;
    for await (const chunk of request) {
      total += chunk.length;
      if (total > maxBodyBytes) {
        response.writeHead(413).end();
        return;
      }
      chunks.push(chunk);
    }

    const forwarded = await fetch(`${upstream}${webhookPath}${url.search}`, {
      method: 'POST',
      headers: {
        'content-type': request.headers['content-type'] ?? 'application/json',
        'x-signature': request.headers['x-signature'] ?? '',
        'x-request-id': request.headers['x-request-id'] ?? ''
      },
      body: Buffer.concat(chunks)
    });
    response.writeHead(forwarded.status).end(await forwarded.text());
  } catch {
    response.writeHead(502).end();
  }
}).listen(8091, '127.0.0.1', () => {
  process.stdout.write('Webhook proxy listening on http://127.0.0.1:8091\n');
});
