# RAPI - REST API for BMC Remedy AR System

RAPI is a REST API proxy that provides a simple HTTP interface to the BMC Remedy AR System. It translates standard HTTP methods (GET, POST, PUT, PATCH, DELETE) into AR System API calls, allowing any HTTP client to interact with Remedy forms without requiring the native AR System SDK.

## Requirements

- JDK 17
- Gradle 7.6.4 (included via wrapper)
- Network access to a BMC Remedy AR System server

## Quick Start

### Local Development

```bash
./gradlew bootRun -Dgrails.server.port=8080
```

### Docker

```bash
docker build -t rapi .
docker run -p 8080:8080 rapi
```

## Authentication

All API requests require **HTTP Basic Authentication**. The provided credentials are passed through to the AR System server to authenticate against Remedy.

```bash
curl -u "username:password" http://localhost:8080/myserver/MyForm
```

## API Reference

The URL structure follows the pattern:

```
http://host:port/{server}/{form}/{query}
```

- **server** - The AR System server hostname
- **form** - The Remedy form (schema) name
- **query** - An AR System qualification string

### Optional Query Parameters

| Parameter | Default | Description |
|---|---|---|
| `port` | `0` | AR System server port |
| `format` | `JSON` | Response format: `JSON` or `XML` |
| `fieldNames` | `true` | Return field names instead of field IDs |
| `translateSelectionFields` | `true` | Translate selection field IDs to display values |
| `fields` | all | Comma-separated list of field names or IDs to return |
| `firstEntry` | `0` | Index of the first record to return (pagination) |
| `maxEntries` | `0` (all) | Maximum number of records to return |
| `sort` | none | Sort fields, e.g. `Status,-CreateDate` (prefix `-` for descending) |
| `dateFormat` | `EEE MMM dd HH:mm:ss zzz yyyy` | Java SimpleDateFormat pattern for dates |
| `countOnly` | `false` | Return only the record count |
| `cacheResults` | `false` | Cache query results in memory |
| `cacheTime` | `600000` | Cache duration in milliseconds |
| `showDisplayOnlyFields` | `false` | Include display-only fields in results |
| `formDetails` | `false` | Return detailed form metadata (with form list) |
| `showServerConfig` | `false` | Return server configuration |
| `showServerStatistics` | `false` | Return server statistics |
| `impersonateUser` | none | Impersonate a different AR System user |
| `rpcQueue` | none | Use a specific RPC queue |

---

### GET - Query Records

#### List all forms on a server

```bash
curl -u user:pass http://localhost:8080/myserver
```

**Response:**
```json
{
  "status": "success",
  "forms": ["Form1", "Form2", "Form3"]
}
```

#### List all forms with details

```bash
curl -u user:pass "http://localhost:8080/myserver?formDetails=true"
```

#### Get field definitions of a form

```bash
curl -u user:pass http://localhost:8080/myserver/HPD:Help%20Desk
```

**Response:**
```json
{
  "status": "success",
  "form": "HPD:Help Desk",
  "fields": [
    {
      "name": "Incident Number",
      "fieldId": 1000000161,
      "type": "CharacterField",
      "entryMode": "Optional",
      "valueMapping": null
    }
  ]
}
```

#### Query records

```bash
curl -u user:pass "http://localhost:8080/myserver/HPD:Help%20Desk/'Status'=\"Assigned\""
```

**Response:**
```json
{
  "status": "success",
  "query": "'Status'=\"Assigned\"",
  "server": "myserver:0",
  "form": "HPD:Help Desk",
  "runtime": 234,
  "dataSize": 5,
  "data": [
    {
      "id": "INC000000000123",
      "values": {
        "Incident Number": "INC000000000123",
        "Status": "Assigned",
        "Summary": "Example incident"
      }
    }
  ]
}
```

#### Query with pagination and sorting

```bash
curl -u user:pass "http://localhost:8080/myserver/HPD:Help%20Desk/'Status'=\"Assigned\"?firstEntry=0&maxEntries=10&sort=-CreateDate"
```

#### Query with specific fields

```bash
curl -u user:pass "http://localhost:8080/myserver/HPD:Help%20Desk/'1'!=%24NULL%24?fields=Incident%20Number,Status,Summary"
```

#### Count records only

```bash
curl -u user:pass "http://localhost:8080/myserver/HPD:Help%20Desk/'Status'=\"Assigned\"?countOnly=true"
```

**Response:**
```json
{
  "form": "HPD:Help Desk",
  "query": "'Status'=\"Assigned\"",
  "dataSize": 42,
  "runtime": 89
}
```

#### Query via POST body

```bash
curl -u user:pass -X POST http://localhost:8080/search \
  -H "Content-Type: application/json" \
  -d '{
    "server": "myserver",
    "form": "HPD:Help Desk",
    "query": "'Status'=\"Assigned\""
  }'
```

#### Get server configuration

```bash
curl -u user:pass "http://localhost:8080/myserver?showServerConfig=true"
```

#### Get server statistics

```bash
curl -u user:pass "http://localhost:8080/myserver?showServerStatistics=true"
```

---

### POST - Create Records

Create one or more new records on a form. The request body is a JSON object where each key is a label (returned in the response) and each value contains the field data.

```bash
curl -u user:pass -X POST http://localhost:8080/myserver/HPD:Help%20Desk \
  -H "Content-Type: application/json" \
  -d '{
    "entry1": {
      "Summary": "New incident from API",
      "Status": "New",
      "Impact": "4-Minor/Localized"
    }
  }'
```

**Response:**
```json
{
  "entry1": "INC000000000456"
}
```

Fields can be referenced by **name** or **field ID**.

---

### PUT - Update Records

Update an existing record. Identify the record by ID or by query.

#### Update by ID

```bash
curl -u user:pass -X PUT http://localhost:8080/myserver/HPD:Help%20Desk \
  -H "Content-Type: application/json" \
  -d '{
    "id": "INC000000000123",
    "values": {
      "Status": "Resolved",
      "Resolution": "Fixed via API"
    }
  }'
```

#### Update by query

```bash
curl -u user:pass -X PUT http://localhost:8080/myserver/HPD:Help%20Desk \
  -H "Content-Type: application/json" \
  -d '{
    "query": "'Incident Number'=\"INC000000000123\"",
    "values": {
      "Status": "Resolved"
    }
  }'
```

#### Multi-match options

When a query matches multiple records, control behavior with `multiMatchOption`:

| Value | Behavior |
|---|---|
| `0` | Error if multiple matches |
| `1` | Update only the first match |
| `2` | Update all matches (default) |

**Response:**
```json
[
  {
    "message": "success",
    "entry": {
      "id": "INC000000000123",
      "values": { "Status": "Resolved" }
    }
  }
]
```

---

### PATCH - Merge Records

Similar to PUT, but uses the AR System merge operation. Useful for upsert-like behavior.

```bash
curl -u user:pass -X PATCH http://localhost:8080/myserver/HPD:Help%20Desk \
  -H "Content-Type: application/json" \
  -d '{
    "id": "INC000000000123",
    "values": {
      "Status": "In Progress"
    },
    "mergeOptions": 1028
  }'
```

The `mergeOptions` parameter controls the merge behavior (default: `1028`). See BMC documentation for available merge option values.

---

### DELETE - Delete Records

Delete all records matching a query.

```bash
curl -u user:pass -X DELETE "http://localhost:8080/myserver/HPD:Help%20Desk/'Incident Number'=\"INC000000000123\""
```

**Response:**
```json
{
  "INC000000000123": "success"
}
```

Pagination parameters (`firstEntry`, `maxEntries`) can be used to limit which matched records are deleted.

---

### Attachments

#### Download an attachment

```
GET /{server}/{form}/getAttachment/{entryId}/{fieldId}
```

```bash
curl -u user:pass -o attachment.pdf \
  http://localhost:8080/myserver/HPD:Help%20Desk/getAttachment/INC000000000123/600001
```

#### Upload an attachment

```
POST /{server}/{form}/setAttachment/{entryId}/{fieldId}
```

```bash
curl -u user:pass -X POST \
  -F "file=@/path/to/document.pdf" \
  http://localhost:8080/myserver/HPD:Help%20Desk/setAttachment/INC000000000123/600001
```

Maximum file size: **200 MB**.

---

## Response Formats

All endpoints support JSON (default) and XML output. Set `format=XML` to receive XML responses.

```bash
curl -u user:pass "http://localhost:8080/myserver/MyForm/'1'!=%24NULL%24?format=XML"
```

## Date Formats

Dates are returned in the format specified by the `dateFormat` parameter. The following input formats are supported when creating or updating records:

- `EEE MMM dd HH:mm:ss zzz yyyy` (default, e.g. `Mon Jan 01 12:00:00 CET 2024`)
- `yyyy-MM-dd'T'HH:mm:ss'Z'` (ISO 8601)
- `yyyy-MM-dd'T'HH:mm:ss`
- `yyyy-MM-dd HH:mm:ss`
- `dd.MM.yyyy'T'HH:mm:ss'Z'`
- `dd.MM.yyyy'T'HH:mm:ss`
- `dd.MM.yyyy HH:mm:ss`

## Error Handling

On error, the API returns HTTP 500 with an error message:

```json
{
  "status": "error",
  "message": "ARException: Entry does not exist in database"
}
```

## Field Caching

Field definitions are cached in memory for 10 minutes per server/user/form combination. This reduces round-trips to the AR System server for repeated queries against the same form.

