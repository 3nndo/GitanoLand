# Entorno de compilacion del AppImage (Ubuntu 22.04: glibc antigua = funciona en casi todas las distros actuales).
FROM ubuntu:22.04
ENV DEBIAN_FRONTEND=noninteractive LANG=C.UTF-8
RUN apt-get update && apt-get install -y --no-install-recommends \
      python3 python3-pip python3-venv python3-dev ca-certificates curl file binutils squashfs-tools desktop-file-utils \
      xvfb x11-utils x11-apps imagemagick xdotool \
      libnss3 libnspr4 libxkbcommon0 libxkbcommon-x11-0 libegl1 libgl1 libopengl0 libdbus-1-3 libfontconfig1 libfreetype6 \
      libxcomposite1 libxdamage1 libxrandr2 libxtst6 libxi6 libxcursor1 libxss1 libasound2 libglib2.0-0 libxcb-cursor0 \
      libxcb-icccm4 libxcb-image0 libxcb-keysyms1 libxcb-render-util0 libxcb-shape0 libxcb-xinerama0 libxcb-xkb1 \
      libatk1.0-0 libatk-bridge2.0-0 libcups2 libdrm2 libgbm1 libpango-1.0-0 libcairo2 libxfixes3 libxext6 libx11-xcb1 \
    && rm -rf /var/lib/apt/lists/*
RUN pip3 install --no-cache-dir -U pip && pip3 install --no-cache-dir pywebview==6.2.1 PyQt6 PyQt6-WebEngine qtpy pyinstaller
WORKDIR /build
