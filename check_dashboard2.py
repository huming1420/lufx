import sqlite3

conn = sqlite3.connect(r'c:\great\陆控\backend\data\insight.db')
conn.row_factory = sqlite3.Row

print("=== Job 8d8b6c725850 items ===")
items = conn.execute(
    "SELECT insight_type, card_id, status, error_message, finished_at "
    "FROM insight_job_item WHERE job_id='8d8b6c725850' ORDER BY insight_type, card_id"
).fetchall()

for i in items:
    print(f"  {i['insight_type']:12s} {i['card_id']:25s} status={i['status']:10s} error={i['error_message'][:40] if i['error_message'] else '':40s} finished={i['finished_at'][:16] if i['finished_at'] else 'None'}")

print("\n=== All insight_result for period 2026-03-YTD ===")
rows = conn.execute(
    "SELECT insight_type, card_id, status, standard_insight IS NOT NULL as has_content "
    "FROM insight_result WHERE period='2026-03-YTD'"
).fetchall()
for r in rows:
    print(f"  {r['insight_type']:12s} {r['card_id']:25s} status={r['status']:10s} has_content={r['has_content']}")
