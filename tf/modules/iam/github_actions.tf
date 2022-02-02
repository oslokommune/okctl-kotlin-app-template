locals {
  tags = {
    Team = "okctl-reference-app"
    Env  = "dev"
  }
}

# For thumbprint_list value: see `tf/bin/generate_fingerprint_for_oidc.sh`
resource "aws_iam_openid_connect_provider" "github_oidc" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
  tags            = local.tags
}

# See documentation in `./github_ecr_push/README.md`
module "okctl_reference_app" {
  source            = "./github_ecr_push"
  oidc_provider_arn = aws_iam_openid_connect_provider.github_oidc.arn
  tags              = local.tags

  # Update the following variables
  github_account = "oslokommune"
  name           = "okctl-reference-app"
  ecr_repo       = "okctl-reference-app"
  github_repo    = "okctl-reference-app"
}