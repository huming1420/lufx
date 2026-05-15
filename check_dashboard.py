import sqlite3

conn = sqlite3.connect(r'c:\great\陆控\backend\data\insight.db')
conn.row_factory = sqlite3.Row

print("=== Dashboard Insight Result ===")
row = conn.execute(
    "SELECT status, standard_insight, error_message, started_at, finished_at, updated_at "
    "FROM insight_result WHERE period='2026-03-YTD' AND insight_type='dashboard'"
).fetchone()
if row:
    print(f"status={row['status']}")
    print(f"standard_insight={row['standard_insight'][:80] if row['standard_insight'] else 'None'}")
    print(f"error_message={row['error_message']}")
    print(f"started_at={row['started_at']}")
    print(f"finished_at={row['finished_at']}")
    print(f"updated_at={row['updated_at']}")
else:
    print("No dashboard insight found!")

print("\n=== Active Jobs (dashboard type) ===")
rows = conn.execute(
    "SELECT job_id, job_type, status, total_count, finished_count, failed_count, created_at "
    "FROM insight_job WHERE period='2026-03-YTD' AND job_type='dashboard' AND status IN ('pending','running')"
).fetchall()
for r in rows:
    print(f"  job_id={r['job_id'][:8]}... status={r['status']} total={r['total_count']} finished={r['finished_count']} failed={r['failed_count']}")

print("\n=== All Active Jobs ===")
rows = conn.execute(
    "SELECT job_id, job_type, status, total_count, finished_count, failed_count, created_at "
    "FROM insight_job WHERE period='2026-03-YTD' AND status IN ('pending','running')"
).fetchall()
for r in rows:
    print(f"  job_id={r['job_id'][:8]}... job_type={r['job_type']:10s} status={r['status']} total={r['total_count']} finished={r['finished_count']} failed={r['failed_count']}")

print("\n=== Job Items for active jobs ===")
for r in rows:
    items = conn.execute(
        "SELECT insight_type, card_id, status, error_message "
        "FROM insight_job_item WHERE job_id=?",
        (r['job_id'],)
    ).fetchall()
    for i in items:
        print(f"    {i['insight_type']:12s} {i['card_id']:25s} status={i['status']:10s} error={i['error_message'][:30] if i['error_message'] else ''}")
