import sqlite3
db = 'backend/data/insight.db'
conn = sqlite3.connect(db)
c = conn.cursor()
c.execute("DELETE FROM insight_result WHERE insight_type='dashboard'")
c.execute("DELETE FROM insight_job_item WHERE insight_type='dashboard'")
conn.commit()
conn.close()
print('Deleted dashboard cache')