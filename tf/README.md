Terraform for okctl-reference-app
=====

# AWS configuration
Set up credentials in `~/.aws/crendentials` or authenticate with [aws sso](https://www.okctl.io/authenticating-to-aws/#aws-single-sign-on-sso)

Region configuration is done in [variables.tf](variables.tf)

Note: The code is currently not set up to consider dev/prod environment, and relies on the logged in aws user.
If you need to change from dev to prod you will need to do a separate login to that environment before executing `terraform apply`

# Terraform

* `terraform init`
* `terraform plan`
* `terraform apply`

# modules/iam/github_ecr_push
Create resources for enabling a github account to push images to ECR

Update [github_actions.tf](modules/iam/github_actions.tf) with your repo/account configuration

Will create three resources in your account
* `module.iam.aws_iam_openid_connect_provider.github_oidc`
  * The OIDC provider that connects github with your account
  * https://console.aws.amazon.com/iamv2/home#/identity_providers
  * *NOTE*: the expiry date on the current `thumbprint_list` will expire, and needs to be updated when it does, there are no automatic update of the thumbprint list (see bin/generate_fingerprint_for_oidc.sh for script that is used to generate the thumbprint)
* `module.iam.module.okctl-reference-app.aws_iam_role.this`
  * The role github workflow will assume
  * https://console.aws.amazon.com/iamv2/home#/roles - search for `okctl-reference-app`
* `module.iam.module.okctl-reference-app.data.aws_iam_policy_document.assume_role`
  * See the `Trust relationships` tab for the role above

See [documentation](modules/iam/github_ecr_push/README.md) for more information on setup and github workflow configuration
