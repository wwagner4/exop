# Run it using docker (podman)

podman build -t exop

podman run -v <OUT_DIR>:/out exop --help

podman run -v <OUT_DIR>:/out exop i01
# Keep in mind: The default output of 'exop' is /out

# e.g.
podman run -v /home/wwagner4/work/exop-out:/out exop i01
podman run -v $(pwd):/out exop i01
