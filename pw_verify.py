# -*- coding: utf-8 -*-
"""Playwright 安装验证脚本：打开前端首页并截图"""
from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1440, "height": 900})
    page.goto("http://localhost:3000", timeout=30000)
    page.wait_for_load_state("networkidle")
    page.screenshot(path=r"F:\Java\智能招聘匹配平台\playwright_verify.png")
    print("TITLE:", page.title())
    print("URL:", page.url)
    browser.close()
print("OK")
