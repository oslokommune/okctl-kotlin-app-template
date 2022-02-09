Terraform for okctl-reference-app
=====

This Terraform configuration enables a Github repository to push images to Elastic Container Repository (ECR) in your AWS account while authenticating using OpenID Connect. This avoids having to configure (long-lived) IAM access keys as secrets in Github.

Terraform will output the IAM role [ARN](https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html) which you can use to configure your Github action.

The example in [main.tf](main.tf) shows the configuration for the [Okctl reference application](https://www.okctl.io/set-up-a-reference-application-full-example/). See documentation of the [github_ecr_push](modules/github_ecr_push/README.md) module for more information on github workflow configuration and setting up your own application.

# AWS configuration

Set up credentials in `~/.aws/crendentials` or authenticate with [aws sso](https://www.okctl.io/authenticating-to-aws/#aws-single-sign-on-sso)

Region configuration is done in [variables.tf](variables.tf)

*NOTE*: The code is currently not set up to consider dev/prod environment, and relies on the logged in aws user.
If you need to change from dev to prod you will need to do a separate login to that environment before executing `terraform apply`.


# Applying the Terraform configuration

* `terraform init`
* `terraform plan`
* `terraform apply`


# Terraform modules

Terraform modules are located in the [modules](modules) directory.

## [github_oidc_provider](modules/github_oidc_provider)

Creates an [OIDC identity provider](https://console.aws.amazon.com/iamv2/home#/identity_providers) that connects Github with your AWS account.

*NOTE*: The Github OIDC provider certificate `thumbprint` should not change, but this [did happen recently](https://github.blog/changelog/2022-01-13-github-actions-update-on-oidc-based-deployments-to-aws/) (although unintentionally). If they need to be updated, you can pass them in the `thumbprints` variable argument to the module. See [bin/generate_fingerprint_for_oidc.sh](bin/generate_fingerprint_for_oidc.sh) for a script that can generate the thumbprint.

## [github_ecr_push](modules/github_ecr_push)

Creates an [IAM role](https://console.aws.amazon.com/iamv2/home#/roles) which your Github workflow will assume in order to push images to your ECR repository.

You should create one instance of this module for each Github repository you wish to push images from.
