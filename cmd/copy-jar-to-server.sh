echo $(date)
docker cp ../target/photographer-1.0.0.jar mcserver:/data/plugins
docker exec mcserver chown dockeruser:dockergroup /data/plugins -R
