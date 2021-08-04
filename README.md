# Running locally

Run:

```shell
make run
```


Test that the application works:
```shell
curl http://localhost:8080
```

Run tests
```shell
./gradlew test
```

# Deploy

TODO:
* Explain how to setup github secrets to make github actions work.
* Explain or refer to doc how ArgoCD deploys
* Explain how securitygrouppolicy has to be set up
In example:
```
sg-0ce6d1c5f249214b7 - okctltemplatedevokctl-template-devRDSPostgresOutgoing

Inncomming:
sgr-06ff5be8911ac9bc5	IPv4	DNS (UDP)	UDP	53	192.168.2.0/24	<-- VPC CDIR RANGE
sgr-0bca48474aec033f0	IPv4	Custom TCP	TCP	8080	192.168.0.0/20 <-- Cluster CDIR range(?)

Outgoing:
sgr-06ab3a6e6964c9bc3	IPv4	HTTPS	TCP	443	0.0.0.0/0 <-- All https traffic

```
