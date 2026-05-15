import sqlite3
import time

conn = sqlite3.connect(r'c:\great\陆控\backend\data\insight.db')
conn.row_factory = sqlite3.Row

print("=== Job a0597c950707 status ===")
row = conn.execute(
    "SELECT status, total_count, finished_count, failed_count FROM insight_job WHERE job_id='a0597c950707'"
).fetchone()
if row:
    print(f"  status={row['status']} total={row['total_count']} finished={row['finished_count']} failed={row['failed_count']}")

print("\n=== Job items ===")
rows = conn.execute(
    "SELECT insight_type, card_id, status, error_message FROM insight_job_item WHERE job_id='a0597c950707'"
).fetchall()
for r in rows:
    print(f"  {r['insight_type']:12s} {r['card_id']:25s} status={r['status']:10s}")

print("\n=== Updated insight_results ===")
rows = conn.execute(
    "SELECT insight_type, card_id, status FROM insight_result WHERE period='2026-03-YTD' AND card_id IN ('vintage','dpd_product','roa','')"
).fetchall()
for r in rows:
    print(f"  {r['insight_type']:12s} {r['card_id']:25s} status={r['status']}")
