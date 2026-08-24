docker cp ../src/main/resources/color-mapping.config mcserver:/data/plugins/Photographer
docker exec mcserver chown dockeruser:dockergroup /data/plugins -R
