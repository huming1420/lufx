import sqlite3

conn = sqlite3.connect(r'c:\great\陆控\backend\data\insight.db')
conn.row_factory = sqlite3.Row

print("=== Failed insight_results ===")
rows = conn.execute(
    "SELECT insight_type, card_id, status, error_message, standard_insight IS NOT NULL as has_content "
    "FROM insight_result WHERE period='2026-03-YTD' AND status='failed'"
).fetchall()
for r in rows:
    print(f"  {r['insight_type']:12s} {r['card_id']:25s} error={r['error_message'][:40] if r['error_message'] else '':40s} has_content={r['has_content']}")

print("\n=== All cards status ===")
rows = conn.execute(
    "SELECT insight_type, card_id, status, error_message "
    "FROM insight_result WHERE period='2026-03-YTD' ORDER BY insight_type, card_id"
).fetchall()
for r in rows:
    print(f"  {r['insight_type']:12s} {r['card_id']:25s} status={r['status']:10s}")
