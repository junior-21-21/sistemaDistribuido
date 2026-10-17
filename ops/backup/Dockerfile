FROM postgres:15-alpine
RUN apk add --no-cache openssl
COPY backup-loop.sh /usr/local/bin/backup-loop.sh
COPY verify-restore.sh /usr/local/bin/verify-restore.sh
RUN chmod 0555 /usr/local/bin/backup-loop.sh /usr/local/bin/verify-restore.sh
ENTRYPOINT ["/usr/local/bin/backup-loop.sh"]
