import sqlite3, json
conn = sqlite3.connect(r'c:\great\陆控\backend\data\insight.db')
conn.row_factory = sqlite3.Row

print("=== Orphan job items (running/pending with ready results) ===")
rows = conn.execute("""
    SELECT i.insight_type, i.card_id, i.status as item_status, r.status as result_status
    FROM insight_job_item i
    LEFT JOIN insight_result r ON r.period='2026-03-YTD'
        AND r.insight_type=i.insight_type AND r.card_id=i.card_id
        AND r.metric_version='mock_v2' AND r.prompt_version='v2'
    WHERE i.job_id='8d8b6c725850'
""").fetchall()
for r in rows:
    print(f"  {r['insight_type']:12s} {r['card_id']:25s} item={r['item_status']:10s} result={r['result_status']}")

print("\n=== All jobs ===")
jobs = conn.execute("SELECT job_id, status, total_count, finished_count, failed_count FROM insight_job").fetchall()
for j in jobs:
    print(f"  {j['job_id']} status={j['status']} total={j['total_count']} finished={j['finished_count']} failed={j['failed_count']}")
