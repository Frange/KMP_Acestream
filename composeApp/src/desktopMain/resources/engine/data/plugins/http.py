#-plugin-sig:Qp8JI+UJZECT91bi+DyOOzN9dZmkjYKaU9aSzrbETkULyVOSLn5XSUFEMG6aIfLlHiWtLetnkmg+w3cF78203IcX/MCdWxN+paPbI8iG9GcYTyd2dyAlzH9Pm4OLJc0zevFHQWffSTU/QPKf1LedgZaK6vWp7Hkcnz0rop/hhQbOdXtGXfaA9wYrrRpvbgoEdqnXUz1Yk0aJeAtwmSKLKM66fZrTE0dbzDTV5iB+/oR2zlXHEOqaf5lVlmsJl1Koj18U7H6dETSqsTxlFIDTCAi1qj78JO64EjQyp/7plmF6DEYct9AIref60wzT4Kp3HRHqX9ui3R0/Z1U3dEqVxQ==
import logging
import re

from streamlink.plugin import Plugin, pluginmatcher
from streamlink.plugin.plugin import parse_params
from streamlink.stream.http import HTTPStream
from streamlink.utils.url import update_scheme


log = logging.getLogger(__name__)


@pluginmatcher(re.compile(
    r"httpstream://(?P<url>\S+)(?:\s(?P<params>.+))?$",
))
class HTTPStreamPlugin(Plugin):
    def _get_streams(self):
        data = self.match.groupdict()
        url = update_scheme("https://", data.get("url"), force=False)
        params = parse_params(data.get("params"))
        log.debug(f"URL={url}; params={params}")

        return {"live": HTTPStream(self.session, url, **params)}


__plugin__ = HTTPStreamPlugin
