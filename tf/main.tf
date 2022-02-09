locals {
  tags = {
    Team = "okctl-reference-app"
    Env  = "dev"
  }
}

module "github_oidc" {
  source = "./modules/github_oidc_provider"

  tags = local.tags
}

# See documentation in `./modules/github_ecr_push/README.md`
module "okctl_reference_app" {
  source = "./modules/github_ecr_push"

  oidc_provider_arn = module.github_oidc.provider_arn
  tags              = local.tags

  # Update the following variables
  name           = "okctl-reference-app"
  github_account = "oslokommune"
  github_repo    = "okctl-reference-app"
  ecr_repo       = "okctl-reference-app"
}
