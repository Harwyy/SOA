<?php
declare(strict_types=1);

// Same-origin proxy for the static UI hosted in public_html. Only the two
// application APIs below are available; it is not an open proxy.
$targets = [
    'city' => 'https://helios.se.ifmo.ru:18081/city-service',
    'route' => 'https://127.0.0.1:20000/route-service',
];

$service = $_GET['service'] ?? '';
$path = $_GET['path'] ?? '';
$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
$methodOverride = strtoupper($_SERVER['HTTP_X_HTTP_METHOD_OVERRIDE'] ?? '');
if ($method === 'POST' && in_array($methodOverride, ['PUT', 'DELETE'], true)) {
    // public_html disallows PUT and DELETE, while the City API needs them for
    // record updates and collection operations. Only these two methods can be
    // restored for the application-server upstream.
    $method = $methodOverride;
} elseif ($methodOverride !== '') {
    http_response_code(400);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['message' => 'Invalid HTTP method override']);
    exit;
}
if (!isset($targets[$service]) || !is_string($path) || !str_starts_with($path, '/')) {
    http_response_code(400);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['message' => 'Invalid API request']);
    exit;
}

if (($service === 'city' && !str_starts_with($path, '/cities'))
    || ($service === 'route' && !str_starts_with($path, '/route/calculate/'))) {
    http_response_code(403);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['message' => 'API path is not allowed']);
    exit;
}

$requestHeaders = ['Accept: application/json'];
if (isset($_SERVER['CONTENT_TYPE'])) {
    $requestHeaders[] = 'Content-Type: ' . $_SERVER['CONTENT_TYPE'];
}

$context = stream_context_create([
    'http' => [
        'method' => $method,
        'header' => implode("\r\n", $requestHeaders),
        'content' => file_get_contents('php://input'),
        'ignore_errors' => true,
        'timeout' => 15,
    ],
    'ssl' => [
        // Both application services use deployment-specific self-signed TLS.
        'verify_peer' => false,
        'verify_peer_name' => false,
    ],
]);

$body = @file_get_contents($targets[$service] . $path, false, $context);
$status = 502;
if (isset($http_response_header[0]) && preg_match('/\s(\d{3})\s/', $http_response_header[0], $matches)) {
    $status = (int) $matches[1];
}

http_response_code($status);
header('Content-Type: application/json; charset=utf-8');
echo $body === false ? json_encode(['message' => 'Upstream service is unavailable']) : $body;
