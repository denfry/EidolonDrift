#!/usr/bin/env python3
"""Minimal Source-RCON client for driving the Eidolon Drift dev server headlessly.

Lets the agent / CI run /eidolon commands against a live Minecraft world and read
the responses — the practical way to "enter the game and check how it works"
without a display.

Usage:
    python tools/rcon.py "eidolon status"                 # run one command
    python tools/rcon.py "eidolon status" "eidolon reload" # run several in order
    python tools/rcon.py --setup                            # prep run/server.properties for RCON
    python tools/rcon.py --host 127.0.0.1 --port 25575 --password eidolon_drift "list"

Defaults match tools/dev-server.properties (port 25575, password eidolon_drift).
Commands are sent WITHOUT a leading slash (RCON convention).
"""
import argparse
import os
import select
import socket
import struct
import sys

TYPE_AUTH = 3
TYPE_AUTH_RESPONSE = 2
TYPE_COMMAND = 2
TYPE_RESPONSE = 0

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Server responses are UTF-8 (em-dashes, etc.); make sure we print them cleanly
# even on a legacy code-page Windows console.
for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8")
    except (AttributeError, ValueError):
        pass


def _encode(req_id: int, ptype: int, body: str) -> bytes:
    payload = struct.pack("<ii", req_id, ptype) + body.encode("utf-8") + b"\x00\x00"
    return struct.pack("<i", len(payload)) + payload


def _read_exact(sock: socket.socket, n: int) -> bytes:
    data = b""
    while len(data) < n:
        chunk = sock.recv(n - len(data))
        if not chunk:
            raise ConnectionError("RCON connection closed early")
        data += chunk
    return data


def _read_packet(sock: socket.socket):
    (length,) = struct.unpack("<i", _read_exact(sock, 4))
    payload = _read_exact(sock, length)
    req_id, ptype = struct.unpack("<ii", payload[:8])
    body = payload[8:-2].decode("utf-8", errors="replace")
    return req_id, ptype, body


def run_commands(host: str, port: int, password: str, commands, timeout: float = 10.0) -> int:
    with socket.create_connection((host, port), timeout=timeout) as sock:
        sock.settimeout(timeout)

        # Authenticate.
        sock.sendall(_encode(1, TYPE_AUTH, password))
        req_id, _, _ = _read_packet(sock)
        if req_id == -1:
            print("RCON auth failed: wrong password", file=sys.stderr)
            return 2

        rc = 0
        for cmd in commands:
            cmd = cmd.lstrip("/")
            sock.sendall(_encode(2, TYPE_COMMAND, cmd))
            # Drain the (possibly multi-packet) response with a short idle window.
            body = ""
            while True:
                ready = select.select([sock], [], [], 0.4)[0]
                if not ready:
                    break
                _, _, part = _read_packet(sock)
                body += part
            body = body.strip()
            print(f"> /{cmd}")
            print(body if body else "(no output)")
            print("-" * 50)
        return rc


def setup_run_properties() -> int:
    """Ensure run/server.properties carries the RCON + dev keys (merge, keep others)."""
    template = os.path.join(REPO_ROOT, "tools", "dev-server.properties")
    run_dir = os.path.join(REPO_ROOT, "run")
    os.makedirs(run_dir, exist_ok=True)
    target = os.path.join(run_dir, "server.properties")

    desired = {}
    with open(template, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                desired[k.strip()] = v.strip()

    existing = {}
    order = []
    if os.path.exists(target):
        with open(target, "r", encoding="utf-8") as f:
            for line in f:
                raw = line.rstrip("\n")
                if raw and not raw.startswith("#") and "=" in raw:
                    k, v = raw.split("=", 1)
                    existing[k.strip()] = v.strip()
                    order.append(k.strip())

    for k, v in desired.items():
        if k not in existing:
            order.append(k)
        existing[k] = v

    with open(target, "w", encoding="utf-8") as f:
        f.write("# Generated/merged by tools/rcon.py --setup\n")
        for k in order:
            f.write(f"{k}={existing[k]}\n")

    print(f"Prepared {target} with RCON enabled (port {desired.get('rcon.port')}).")
    return 0


def main() -> int:
    ap = argparse.ArgumentParser(description="RCON client for the Eidolon Drift dev server")
    ap.add_argument("commands", nargs="*", help="commands to run (without leading slash)")
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", type=int, default=25575)
    ap.add_argument("--password", default="eidolon_drift")
    ap.add_argument("--setup", action="store_true",
                    help="prepare run/server.properties for RCON, then exit")
    args = ap.parse_args()

    if args.setup:
        return setup_run_properties()

    if not args.commands:
        ap.error("no commands given (or use --setup)")

    try:
        return run_commands(args.host, args.port, args.password, args.commands)
    except (ConnectionError, OSError) as e:
        print(f"RCON error: {e}\nIs the dev server running? (./gradlew runServer)", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
