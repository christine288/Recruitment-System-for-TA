#!/usr/bin/env python3
"""Capture key UI screenshots for docs/USER_MANUAL.md."""

from pathlib import Path
import sys

try:
    from playwright.sync_api import sync_playwright
except ImportError:
    print("Install Playwright: pip install playwright", file=sys.stderr)
    sys.exit(1)

BASE = "http://localhost:18080/ta-recruitment"
OUT = Path(__file__).resolve().parent.parent / "docs" / "manual-screenshots"
OUT.mkdir(parents=True, exist_ok=True)

TA_USER = "00000000-0000-0000-0000-000000000101"


def login(page, email: str, password: str) -> None:
    page.goto(f"{BASE}/login", wait_until="networkidle")
    page.fill("#email", email)
    page.fill("#password", password)
    page.click("button[type=submit]")
    page.wait_for_load_state("networkidle")


def shot(page, name: str) -> None:
    path = OUT / name
    page.screenshot(path=str(path), full_page=True)
    print(f"Saved {path}")


def launch_browser(p):
    for channel in ("msedge", "chrome"):
        try:
            return p.chromium.launch(channel=channel, headless=True)
        except Exception:
            continue
    return p.chromium.launch(headless=True)


def main() -> int:
    with sync_playwright() as p:
        browser = launch_browser(p)
        context = browser.new_context(viewport={"width": 1440, "height": 900})
        page = context.new_page()

        page.goto(f"{BASE}/login", wait_until="networkidle")
        shot(page, "01-login.png")

        page.goto(f"{BASE}/register", wait_until="networkidle")
        shot(page, "02-register.png")

        login(page, "alice.chen@bupt.local", "ta123")
        page.goto(f"{BASE}/profile", wait_until="networkidle")
        shot(page, "03-profile-view.png")

        page.goto(f"{BASE}/profile?edit=1", wait_until="networkidle")
        shot(page, "04-profile-edit.png")

        page.goto(f"{BASE}/job", wait_until="networkidle")
        shot(page, "05-job-list.png")

        page.goto(f"{BASE}/job?id=1", wait_until="networkidle")
        shot(page, "06-job-detail.png")

        page.goto(f"{BASE}/applications", wait_until="networkidle")
        shot(page, "07-applications.png")

        page.goto(f"{BASE}/logout", wait_until="networkidle")

        login(page, "mo@bupt.local", "mo123")
        page.goto(f"{BASE}/mo", wait_until="networkidle")
        shot(page, "08-mo-dashboard.png")

        page.goto(f"{BASE}/mo/applicant-profile?userId={TA_USER}", wait_until="networkidle")
        shot(page, "09-mo-applicant-profile.png")

        page.goto(f"{BASE}/logout", wait_until="networkidle")

        login(page, "admin@bupt.local", "admin123")
        page.goto(f"{BASE}/admin", wait_until="networkidle")
        shot(page, "10-admin-dashboard.png")

        page.goto(f"{BASE}/admin/ta-profiles", wait_until="networkidle")
        shot(page, "11-admin-ta-profiles.png")

        # User management lives on /admin (POST-only /admin/users returns 405 on GET)
        page.goto(f"{BASE}/admin", wait_until="networkidle")
        management = page.locator("section.card").filter(has_text="User Management")
        management.scroll_into_view_if_needed()
        management.screenshot(path=str(OUT / "12-admin-users.png"))
        print(f"Saved {OUT / '12-admin-users.png'}")

        browser.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
