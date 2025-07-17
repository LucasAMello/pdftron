FROM amazoncorretto:21-al2023

# install wget, tar, xz and gzip in one go
RUN dnf update -y && \
    dnf install -y wget tar xz gzip && \
    rm -rf /var/cache/dnf

RUN mkdir -p /opt/HTML2PDF
WORKDIR /opt/HTML2PDF

# Download and extract HTML2PDF
RUN wget -q https://www.pdftron.com/downloads/HTML2PDFLinux.tar.gz && \
    tar -xf HTML2PDFLinux.tar.gz && \
    rm HTML2PDFLinux.tar.gz

ENV HTML2PDF_PATH="/opt/HTML2PDF"

# Install HTML2PDF dependencies
RUN dnf install -y nss nss-util nspr dbus-libs atk at-spi2-atk libXcomposite  \
    libXdamage libXfixes mesa-libgbm libdrm libxkbcommon

# Create a directory for custom fonts and copy the Inter font
RUN mkdir -p /usr/local/share/fonts/truetype/custom
COPY src/main/resources/static/Inter.ttf /usr/local/share/fonts/truetype/custom/Inter.ttf

# Update the font cache to make the new font available system-wide
RUN fc-cache -f -v

WORKDIR /
RUN mkdir -p /pdfs

ADD target/reports-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java",\
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005",\
            "-Xmx3g",\
            "-Xms3g",\
            "-jar",\
            "app.jar"\
            ]
