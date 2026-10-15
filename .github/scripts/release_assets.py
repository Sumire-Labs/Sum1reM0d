"""Reject incomplete/ambiguous downloads before publishing a GitHub Release."""
import sys
from pathlib import Path

from matrix import matrices


def verify(directory):
    assets = list(Path(directory).glob("*.jar"))
    expected = [item["artifact"] for item in matrices()["publish"]["include"]]
    matched = set()
    for target in expected:
        jars = [jar for jar in assets if jar.name.endswith(f"-{target}.jar")]
        if len(jars) != 1 or jars[0].stat().st_size == 0:
            raise ValueError(f"Expected one nonempty release JAR for {target}: {jars}")
        matched.add(jars[0])
    if set(assets) != matched:
        raise ValueError(f"Unexpected release JARs: {set(assets) - matched}")
    print(f"Verified {len(assets)} release JARs")


if __name__ == "__main__":
    verify(sys.argv[1])
