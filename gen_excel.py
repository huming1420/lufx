# -*- coding: utf-8 -*-
"""
Generate QA-readable test case Excel from JSON data file.
Reads test cases from tc_data.json and produces 陆控项目测试用例.xlsx
"""

import json
import os
from openpyxl import Workbook
from openpyxl.styles import Font, Alignment, PatternFill, Border, Side
from openpyxl.utils import get_column_letter

DATA_FILE = r"C:\great\陆控\tc_data.json"
OUTPUT_FILE = r"C:\great\陆控\陆控项目测试用例.xlsx"

def load_test_cases():
    with open(DATA_FILE, 'r', encoding='utf-8') as f:
        return json.load(f)

def create_excel(test_cases):
    wb = Workbook()

    # ===== Sheet 1: 测试用例总览 =====
    ws1 = wb.active
    ws1.title = "测试用例总览"

    # Header style
    header_font = Font(bold=True, size=11, color="FFFFFF")
    header_fill = PatternFill(start_color="4472C4", end_color="4472C4", fill_type="solid")
    header_align = Alignment(horizontal="center", vertical="center", wrap_text=True)
    thin_border = Border(
        left=Side(style='thin'),
        right=Side(style='thin'),
        top=Side(style='thin'),
        bottom=Side(style='thin')
    )

    headers = ["用例编号", "模块", "测试类", "测试方法", "测试描述(目的)", "前置条件", "预期结果", "优先级"]
    for col_idx, h in enumerate(headers, 1):
        cell = ws1.cell(row=1, column=col_idx, value=h)
        cell.font = header_font
        cell.fill = header_fill
        cell.alignment = header_align
        cell.border = thin_border

    # Priority fills
    p0_fill = PatternFill(start_color="FF6B6B", end_color="FF6B6B", fill_type="solid")  # Red
    p1_fill = PatternFill(start_color="6BA3FF", end_color="6BA3FF", fill_type="solid")  # Blue
    p2_fill = PatternFill(start_color="90EE90", end_color="90EE90", fill_type="solid")  # Green

    # Data rows
    data_align = Alignment(vertical="center", wrap_text=True)
    for idx, tc in enumerate(test_cases, 1):
        row = idx + 1

        # Determine priority (default P2)
        method_name = tc.get("method_name", "")
        desc = tc.get("description", "").lower()
        if any(k in method_name.lower() or k in desc for k in ["success", "happy", "normal", "正常", "成功"]):
            priority = "P0"
            pri_fill = p0_fill
        elif any(k in method_name.lower() or k in desc for k in ["fail", "error", "exception", "invalid", "null", "empty", "异常", "失败", "空"]):
            priority = "P1"
            pri_fill = p1_fill
        else:
            priority = "P2"
            pri_fill = p2_fill

        values = [
            f"TC-{idx:04d}",
            tc.get("module", ""),
            tc.get("class_name", ""),
            method_name,
            tc.get("description", "") or tc.get("purpose", ""),
            "JUnit 4 + Mockito 环境已初始化",
            "断言通过，无异常抛出",
            priority
        ]

        for col_idx, val in enumerate(values, 1):
            cell = ws1.cell(row=row, column=col_idx, value=val)
            cell.alignment = data_align
            cell.border = thin_border
            if col_idx == 8:  # Priority column
                cell.fill = pri_fill
                cell.alignment = Alignment(horizontal="center", vertical="center")
            elif col_idx == 1:
                cell.alignment = Alignment(horizontal="center", vertical="center")

    # Column widths
    col_widths = [10, 12, 35, 40, 55, 30, 28, 10]
    for i, w in enumerate(col_widths, 1):
        ws1.column_dimensions[get_column_letter(i)].width = w

    ws1.freeze_panes = "A2"

    # ===== Sheet 2: 汇总统计 =====
    ws2 = wb.create_sheet(title="汇总统计")

    # Build per-class stats
    class_stats = {}
    for tc in test_cases:
        cn = tc.get("class_name", "Unknown")
        mod = tc.get("module", "Unknown")
        key = cn
        if key not in class_stats:
            class_stats[key] = {"模块": mod, "总数": 0, "P0": 0, "P1": 0, "P2": 0}
        class_stats[key]["总数"] += 1
        # Recalculate priority same way as above
        mn = tc.get("method_name", "").lower()
        ds = (tc.get("description", "") or tc.get("purpose", "")).lower()
        if any(k in mn or k in ds for k in ["success", "happy", "normal", "正常", "成功"]):
            class_stats[key]["P0"] += 1
        elif any(k in mn or k in ds for k in ["fail", "error", "exception", "invalid", "null", "empty", "异常", "失败", "空"]):
            class_stats[key]["P1"] += 1
        else:
            class_stats[key]["P2"] += 1

    stat_headers = ["序号", "模块", "测试类", "用例总数", "P0(核心)", "P1(重要)", "P2(一般)"]
    for col_idx, h in enumerate(stat_headers, 1):
        cell = ws2.cell(row=1, column=col_idx, value=h)
        cell.font = header_font
        cell.fill = header_fill
        cell.alignment = header_align
        cell.border = thin_border

    total_all = total_p0 = total_p1 = total_p2 = 0
    for s_idx, (cn, stats) in enumerate(sorted(class_stats.items()), 1):
        row = s_idx + 1
        vals = [
            s_idx,
            stats["模块"],
            cn,
            stats["总数"],
            stats["P0"],
            stats["P1"],
            stats["P2"],
        ]
        for col_idx, v in enumerate(vals, 1):
            cell = ws2.cell(row=row, column=col_idx, value=v)
            cell.alignment = Alignment(vertical="center", wrap_text=True)
            cell.border = thin_border
            if col_idx == 1:
                cell.alignment = Alignment(horizontal="center", vertical="center")
        total_all += stats["总数"]
        total_p0 += stats["P0"]
        total_p1 += stats["P1"]
        total_p2 += stats["P2"]

    # Grand total row
    gt_row = len(class_stats) + 2
    gt_vals = ["合计", "", "", total_all, total_p0, total_p1, total_p2]
    gt_font = Font(bold=True)
    gt_fill = PatternFill(start_color="FFF2CC", end_color="FFF2CC", fill_type="solid")
    for col_idx, v in enumerate(gt_vals, 1):
        cell = ws2.cell(row=gt_row, column=col_idx, value=v)
        cell.font = gt_font
        cell.fill = gt_fill
        cell.alignment = Alignment(vertical="center")
        cell.border = thin_border

    stat_col_widths = [8, 12, 38, 12, 12, 12, 12]
    for i, w in enumerate(stat_col_widths, 1):
        ws2.column_dimensions[get_column_letter(i)].width = w
    ws2.freeze_panes = "A2"

    # Save
    os.makedirs(os.path.dirname(OUTPUT_FILE), exist_ok=True)
    wb.save(OUTPUT_FILE)
    print(f"Excel generated successfully: {OUTPUT_FILE}")
    print(f"Total test cases: {len(test_cases)}")
    print(f"Summary: {len(class_stats)} test classes, Total={total_all}, P0={total_p0}, P1={total_p1}, P2={total_p2}")


if __name__ == "__main__":
    tcs = load_test_cases()
    create_excel(tcs)
