import sqlite3
from datetime import datetime, timezone

conn = sqlite3.connect(r'c:\great\陆控\backend\data\insight.db')
conn.row_factory = sqlite3.Row

now = datetime.now(timezone.utc).isoformat()

print("=== Fix pending/running insight_result records ===")
# Update all pending/running insight_results for period 2026-03-YTD that have failed job items
conn.execute(
    """UPDATE insight_result SET status='failed', error_message='任务超时', finished_at=?
        WHERE period='2026-03-YTD' AND status IN ('pending','running')
        AND EXISTS (
            SELECT 1 FROM insight_job_item 
            WHERE job_id='8d8b6c725850' 
            AND insight_result.insight_type=insight_job_item.insight_type 
            AND insight_result.card_id=insight_job_item.card_id
            AND insight_job_item.status='failed'
            AND insight_job_item.error_message='任务超时'
        )""",
    (now,)
)
conn.commit()

# Check results
rows = conn.execute(
    "SELECT insight_type, card_id, status, error_message FROM insight_result WHERE period='2026-03-YTD' AND status IN ('pending','running','failed')"
).fetchall()
for r in rows:
    print(f"  {r['insight_type']:12s} {r['card_id']:25s} status={r['status']:10s} error={r['error_message'][:30] if r['error_message'] else ''}")

print("\nDone!")
