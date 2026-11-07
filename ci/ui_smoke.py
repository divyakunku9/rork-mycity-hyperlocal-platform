#!/usr/bin/env python3
"""
End-to-end smoke test for Shehar Saathi on an Android emulator (run from GitHub Actions).

Signs in with the Supabase test number, picks Hyderabad, visits every tab and the main
sub-screens, files a report, then fires random taps (monkey) to shake out crashes.
Each step prints the visible text and a small base64 JPEG screenshot to the log, between
"=====SHOT <name>=====" and "=====END=====" markers, so results can be read without
downloading artifacts. Exit code is non-zero if the app crashed.
"""
import base64
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG = "com.sheharsaathi.app"
TEST_PHONE = "9999900001"
TEST_CODE = "246810"
OUT = "ui-out"
os.makedirs(OUT, exist_ok=True)
crashes = []
step_no = 0


def sh(cmd, check=False, timeout=60):
    r = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=timeout)
    if check and r.returncode != 0:
        print(f"!! command failed: {cmd}\n{r.stderr}")
    return r.stdout


def adb(cmd, **kw):
    return sh(f"adb {cmd}", **kw)


def dump():
    """Returns the UI hierarchy as a list of (text, desc, (cx, cy), clickable)."""
    for _ in range(3):
        out = adb("exec-out uiautomator dump /dev/tty", timeout=40)
        m = re.search(r"<\?xml.*</hierarchy>", out, re.S)
        if m:
            try:
                root = ET.fromstring(m.group(0))
            except ET.ParseError:
                continue
            nodes = []
            for n in root.iter("node"):
                b = re.findall(r"\d+", n.get("bounds", ""))
                if len(b) != 4:
                    continue
                x1, y1, x2, y2 = map(int, b)
                nodes.append((n.get("text", ""), n.get("content-desc", ""), ((x1 + x2) // 2, (y1 + y2) // 2),
                              n.get("clickable") == "true", (x1, y1, x2, y2)))
            return nodes
        time.sleep(1.5)
    print("!! uiautomator dump failed (screen never idle?)")
    return []


def visible_text(nodes):
    seen, out = set(), []
    for t, d, *_ in nodes:
        for s in (t, d):
            s = (s or "").strip()
            if s and s not in seen:
                seen.add(s)
                out.append(s)
    return out


def check_crash(label):
    log = adb("logcat -d -b crash", timeout=30)
    if PKG in log and "FATAL EXCEPTION" in log:
        print(f"!!!!! CRASH detected after step '{label}':\n{log[-6000:]}")
        crashes.append(label)
        adb("logcat -b crash -c")
        return True
    return False


def shot(name, nodes=None):
    global step_no
    step_no += 1
    tag = f"{step_no:02d}-{name}"
    png = f"{OUT}/{tag}.png"
    with open(png, "wb") as f:
        f.write(subprocess.run("adb exec-out screencap -p", shell=True, capture_output=True).stdout)
    jpg = f"{OUT}/{tag}.jpg"
    if subprocess.run(f"convert {png} -resize 420x -quality 62 {jpg}", shell=True).returncode != 0:
        jpg = png
    nodes = nodes if nodes is not None else dump()
    print(f"----- STEP {tag}: visible text -----")
    print(" | ".join(visible_text(nodes))[:3000])
    with open(jpg, "rb") as f:
        print(f"=====SHOT {tag}=====")
        print(base64.b64encode(f.read()).decode())
        print("=====END=====")
    check_crash(tag)
    sys.stdout.flush()


def find(nodes, *needles, exact=False):
    for needle in needles:
        nl = needle.lower()
        for t, d, c, clickable, _ in nodes:
            for s in (t, d):
                s = (s or "").strip().lower()
                if s and ((s == nl) if exact else (nl in s)):
                    return c
    return None


def tap_text(*needles, wait=8.0, exact=False):
    end = time.time() + wait
    while time.time() < end:
        nodes = dump()
        c = find(nodes, *needles, exact=exact)
        if c:
            adb(f"shell input tap {c[0]} {c[1]}")
            time.sleep(1.5)
            return True
        time.sleep(1)
    print(f"!! could not find {needles}")
    return False


def wait_for(*needles, wait=20.0):
    end = time.time() + wait
    while time.time() < end:
        nodes = dump()
        if find(nodes, *needles):
            return nodes
        time.sleep(1.5)
    print(f"!! timed out waiting for {needles}")
    return None


def type_text(s):
    adb("shell input text " + s.replace(" ", "%s"))
    time.sleep(0.8)


def hide_keyboard():
    adb("shell input keyevent 111")  # ESCAPE closes the IME without leaving the screen
    time.sleep(0.8)


def scroll_down(times=1):
    size = adb("shell wm size")
    w, h = map(int, re.findall(r"(\d+)x(\d+)", size)[-1])
    for _ in range(times):
        adb(f"shell input swipe {w // 2} {int(h * 0.75)} {w // 2} {int(h * 0.3)} 350")
        time.sleep(1.2)


def back():
    adb("shell input keyevent 4")
    time.sleep(1.5)


def main():
    adb("logcat -c")
    adb("logcat -b crash -c")
    adb(f"shell pm grant {PKG} android.permission.ACCESS_FINE_LOCATION")
    adb(f"shell pm grant {PKG} android.permission.ACCESS_COARSE_LOCATION")
    adb("emu geo fix 78.3915 17.4483")  # Madhapur, Hyderabad
    adb(f"shell am start -W -n {PKG}/.MainActivity", timeout=60)
    time.sleep(4)
    shot("launch")

    # ---- Sign in ----
    nodes = wait_for("Sign in to Shehar Saathi", "Send OTP", wait=25)
    shot("signin", nodes)
    tap_text("Phone", exact=True, wait=3)
    if tap_text("Your name", wait=4):
        type_text("CI Tester")
        hide_keyboard()
    if tap_text("Mobile number", wait=4):
        type_text(TEST_PHONE)
        hide_keyboard()
    shot("signin-filled")
    tap_text("Send OTP")
    nodes = wait_for("Enter the 6-digit code", wait=25)
    shot("otp-screen", nodes)
    type_text(TEST_CODE)
    time.sleep(6)
    shot("after-code")

    # ---- City ----
    nodes = wait_for("Hyderabad", "Detect my city", wait=20)
    shot("city-picker", nodes)
    tap_text("Hyderabad", exact=True)
    time.sleep(8)
    shot("explore")
    scroll_down(2)
    shot("explore-scrolled")

    # ---- Tabs ----
    for label, name in [("News Hub", "news"), ("Events", "events"), ("Civic Hub", "civic")]:
        if tap_text(label, exact=True):
            time.sleep(4)
            shot(name)
            scroll_down(2)
            shot(name + "-scrolled")

    # ---- Report flow (no photo) ----
    if tap_text("Report", exact=True):
        time.sleep(3)
        shot("report")
        tap_text("Pothole", wait=4)
        if tap_text("e.g. Large crater", "Describe the issue", wait=5):
            type_text("CI test: pothole near the junction, please ignore")
            hide_keyboard()
        scroll_down(2)
        shot("report-filled")
        tap_text("Submit Report", wait=6)
        time.sleep(8)
        shot("report-submitted")
        back()

    # ---- Civic again: my report should be listed ----
    if tap_text("Civic Hub", exact=True):
        time.sleep(4)
        shot("civic-after-report")

    # ---- Explore sub-screens ----
    if tap_text("Explore", exact=True):
        time.sleep(3)
        for needles, name in [(("Plan a trip", "Where to", "Plan"), "planner"), (("Live board", "Transit", "Departures"), "transit")]:
            if tap_text(*needles, wait=5):
                time.sleep(3)
                shot(name)
                back()
        if tap_text("Profile", "Account", "avatar", wait=5):
            time.sleep(3)
            shot("profile")
            scroll_down(2)
            shot("profile-scrolled")
            back()

    # ---- Restart: session should survive ----
    adb(f"shell am force-stop {PKG}")
    time.sleep(2)
    adb(f"shell am start -W -n {PKG}/.MainActivity", timeout=60)
    time.sleep(8)
    shot("relaunch")

    # ---- Random taps ----
    print("----- monkey: 2000 random events -----")
    out = adb(f"shell monkey -p {PKG} --pct-syskeys 0 --throttle 60 -s 4242 -v 2000", timeout=600)
    print(out[-2500:])
    if "CRASH" in out or "ANR" in out:
        crashes.append("monkey")
    check_crash("monkey")
    adb(f"shell am start -n {PKG}/.MainActivity")
    time.sleep(4)
    shot("after-monkey")

    print("----- app log (warnings and errors) -----")
    pid = adb(f"shell pidof {PKG}").strip()
    log = adb("logcat -d *:W", timeout=60)
    lines = [l for l in log.splitlines() if (pid and f" {pid} " in l) or "AndroidRuntime" in l]
    print("\n".join(lines[-200:]))

    print(f"===== RESULT: {len(crashes)} crash(es) {crashes} =====")
    sys.exit(1 if crashes else 0)


if __name__ == "__main__":
    main()
