# Dataplane SDK Java

A Java SDK for implementing dataplanes based on the
[Dataplane Signaling specification](https://github.com/eclipse-dataplane-signaling/dataplane-signaling/tree/main).
These dataplanes can interact with
[Dataspace Protocol](https://eclipse-dataspace-protocol-base.github.io/DataspaceProtocol/2025-1/) control planes
via the `Dataplane Signaling API`.

For information on how to use the SDK, check out the [getting started guide](./docs/getting-started.md).

## Important Notice

This is an **SDK** — it provides building blocks and abstractions to help you implement a data plane, but it is **not a final product** and is **not production-ready**.

Key responsibilities left to the implementor:

- **Authentication and authorization**: the SDK does not enforce any auth mechanism. You are expected to implement token validation, access control, and any other security checks appropriate for your deployment.
- **Production hardening**: error handling, observability, and operational concerns are not covered out of the box.
- **Deployment configuration**: TLS, network policies, and infrastructure setup are outside the scope of this SDK.

Use this SDK as a foundation and build the necessary production-grade concerns on top of it.
