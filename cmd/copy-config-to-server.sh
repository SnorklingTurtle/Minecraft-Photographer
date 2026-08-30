docker cp ../src/main/resources/config.yml mcserver:/data/plugins/Photographer
docker exec mcserver chown dockeruser:dockergroup /data/plugins -R
