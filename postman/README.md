# Postman test collection

Import `HSF302-Collection.json` and `HSF302-Environment.json` into Postman. Select the environment and set `adminPassword` and `customerPassword` to credentials for the local test database. Default usernames match `DataSeeder`; the exported environment contains no passwords.

Run the folders in order with redirects disabled. The collection checks response status and redirect destinations for the Spring MVC routes. It uses read-only pages plus login/logout; it does not create or delete business data. `baseUrl` defaults to `http://localhost:8080`.

The root collection includes separate folders for public pages, access control, admin pages, customer pages, and session cleanup. Run all folders in order with the Postman Collection Runner or Newman. For Newman, use `newman run postman/HSF302-Collection.json -e postman/HSF302-Environment.json --ignore-redirects` after supplying the two password variables at runtime. Password fields are deliberately empty in the exported environment.

On 2026-11-10, Newman 6.2.3 ran against the local Spring MVC application: **37 requests, 37 test scripts, 74 assertions, 0 failures**. The [PASS screenshot](HSF302-Postman-PASS.png) shows the actual Newman output. The original log and JSON run report are kept in the ignored `local-evidence/assignment02/B/postman/` folder. These requests verify status codes, HTML responses, and redirect targets; they do not test JSON API payloads because this application exposes server-rendered MVC pages.
