import asyncio
import sys
sys.path.insert(0, r'c:\great\陆控\backend\services')

import insight_repo
from insight_job_service import create_insight_job

# Reset failed insights to missing so they will be regenerated
conn = insight_repo._get_conn()

# Reset failed card insights
for card_id in ['vintage', 'dpd_product', 'roa']:
    conn.execute(
        "UPDATE insight_result SET status='pending', error_message='', started_at=NULL, finished_at=NULL "
        "WHERE period='2026-03-YTD' AND insight_type='card' AND card_id=?",
        (card_id,)
    )

# Reset failed dashboard insight
conn.execute(
    "UPDATE insight_result SET status='pending', error_message='', started_at=NULL, finished_at=NULL "
    "WHERE period='2026-03-YTD' AND insight_type='dashboard' AND card_id=''"
)
conn.commit()

print("Reset failed insights to pending status")

# Create a new job for these cards + dashboard
job_id = create_insight_job(
    period='2026-03-YTD',
    scope='all',
    card_ids=['vintage', 'dpd_product', 'roa'],
    force_refresh=True,
)
print(f"Created job: {job_id}")
