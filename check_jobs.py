import sqlite3

conn = sqlite3.connect(r'c:\great\陆控\backend\data\insight.db')
conn.row_factory = sqlite3.Row

print("=== Recent Jobs ===")
rows = conn.execute(
    "SELECT job_id, status, total_count, finished_count, failed_count, created_at, finished_at FROM insight_job WHERE period='2026-03-YTD' ORDER BY created_at DESC LIMIT 5"
).fetchall()

for r in rows:
    finished = r["finished_at"][5:16] if r["finished_at"] else "None"
    print(f'{r["job_id"][:8]}... status={r["status"]:15s} total={r["total_count"]:2d} finished={r["finished_count"]:2d} failed={r["failed_count"]:2d} created={r["created_at"][5:16]} finished={finished}')

print("\n=== Job 8d8b6c725850 Items ===")
items = conn.execute(
    "SELECT insight_type, card_id, status, error_message FROM insight_job_item WHERE job_id='8d8b6c725850' ORDER BY id LIMIT 10"
).fetchall()

for i in items:
    print(f'  {i["insight_type"]:12s} {i["card_id"]:25s} status={i["status"]:10s} error={i["error_message"][:20] if i["error_message"] else ""}')
