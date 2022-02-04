Github Actions push to ECR role
===============================

Roles and policies are scoped to allow a single Github repository to push
to a single ECR repo.


Adding a new project
--------------------

### Terraform config

In `modules/iam/github_actions.tf`:

```terraform
module "friendly_name" {
  source            = "./github_ecr_push"
  name              = "my-project"
  oidc_provider_arn = aws_iam_openid_connect_provider.github_oidc.arn
  tags              = local.tags
}
```

 - `name` is required and used for names for IAM resources. Also used for
   github repo name and ECR repo name if overrides are not specified.
 - `github_repo` is optional, if not specified `name` will be used.
 - `ecr_repo` is optional, if not specified `name` will be used.


### Github action config

In the corresponding github repo, the github action must be set up to use
the newly created role. Figure out the `arn` of the IAM role created by the
module above and add the following snippet to the github action
configuration:

```yaml
jobs:
  deploy:
    name: blabla
    runs-on: ubuntu-latest

    # !!!
    # These permissions are needed to interact with GitHub's OIDC Token endpoint.
    # !!!
    permissions:
      id-token: write
      contents: read

    steps:
      # ... other steps goes here ...

      - uses: aws-actions/configure-aws-credentials@v1
        name: Configure the AWS credentials
        with:
          aws-region: eu-north-1
          role-to-assume: ${{ secrets.DEV_OKCTL_HELLO_ECR_PUSH_ROLE }}

      # ... other steps goes here ...
```

Setting `id-token: write` permission is required to make OIDC work.

The magic is `role-to-assume` used with no other credentials. This will
make the action use Github OIDC instead of secret access keys.