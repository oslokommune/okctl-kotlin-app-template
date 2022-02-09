Github Actions push to ECR role
===============================

Creates an [IAM role](https://console.aws.amazon.com/iamv2/home#/roles)
which your Github workflow will assume in order to push images to your ECR
repository.

Roles and policies are scoped to allow a single Github repository to push
to a single ECR repo. If you wish to push from multiple Github repositories,
create one instance of this module per repository.


Adding a new project
--------------------

### Terraform config

In your `main.tf`:

```terraform
module "github_oidc" {
  source = "./modules/github_oidc_provider"
}

module "friendly_name" {
  source            = "./modules/github_ecr_push"
  name              = "my-project"
  oidc_provider_arn = module.github_oidc.provider_arn
}
```

Configuration options:

- `name`: Name used for resource creation in your AWS account. Required.
- `oidc_provider_arn`: ARN of the OIDC identity provider in your AWS
  account. Required.
- `github_account`: Name of the Github user or organization which contains
  your project repository. Defaults to `oslokommune` if not specified.
- `github_repo`: Name of your Github project repository. If not specified
  the value of `name` will be used.
- `ecr_repo`: Name of your ECR repository. If not specified the value of
  `name` will be used.
- `tags`: Any tags you wish to set on the generated IAM role. Optional.

### Github action config

In the corresponding Github repo, the Github Action that will push the image
to ECR must be set up to use the newly created role. The `ARN` of the IAM
role created by the module will be output by Terraform. Put the value in a
Github Actions secret in your repository, e.g. `ECR_PUSH_ROLE`. Add the
following snippet to your Github Action configuration:

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
          role-to-assume: ${{ secrets.ECR_PUSH_ROLE }}

      # ... other steps goes here ...
```

Setting `id-token: write` permission is required to make OIDC work.

The magic is `role-to-assume` used with no other credentials. This will
make the action use Github OIDC instead of secret access keys.