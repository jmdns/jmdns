# Release Workflow Setup

This document describes the setup and usage of the automated release workflow for jmdns.

## Required GitHub Secrets

Before the release workflow can be used, the following secrets must be configured in the GitHub repository settings:

### Maven Central (OSSRH) Secrets

1. **`OSSRH_USERNAME`**: Your Sonatype OSSRH username
2. **`OSSRH_PASSWORD`**: Your Sonatype OSSRH password or token
3. **`CENTRAL_NAMESPACE`**: Your namespace from the Central Publisher Portal (e.g., `org.jmdns`)

### GPG Signing Secrets

4. **`GPG_PRIVATE_KEY`**: Your private GPG key in ASCII-Armor format
5. **`GPG_PASSPHRASE`**: The passphrase for your GPG key

### Creating and configuring GPG keys

If you don't have a GPG key yet:

```bash
# Generate GPG key
gpg --gen-key

# List keys
gpg --list-secret-keys --keyid-format LONG

# Export private key (ASCII-Armor format)
gpg --armor --export-secret-keys YOUR_KEY_ID

# Upload public key to a key server
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

The exported private key (including `-----BEGIN PGP PRIVATE KEY BLOCK-----` and `-----END PGP PRIVATE KEY BLOCK-----`) should be stored as the `GPG_PRIVATE_KEY` secret.

### Finding your Central Publisher Portal Namespace

1. Log in to the Central Publisher Portal: https://central.sonatype.com/
2. Go to "Publishing" > "Namespaces"
3. Copy your namespace (e.g., `org.jmdns`, `io.github.username`, etc.)
4. Add this as the `CENTRAL_NAMESPACE` secret

**Important**: The namespace must exactly match what is displayed in your portal.

## Using the Release Workflow

### Manual Release Process

1. Go to `Actions` > `Release to Maven Central`
2. Click `Run workflow`
3. Enter the desired versions:
   - **Release version**: e.g., `3.6.3`
   - **Next snapshot version**: e.g., `3.6.4-SNAPSHOT`
4. Click `Run workflow`

### What happens automatically

The workflow performs the following steps:

1. **Set version to release**: Changes the version in `pom.xml` from SNAPSHOT to release version
2. **Build and test**: Compiles and tests the project
3. **Deploy to Maven Central**: Executes `mvn -DskipTests=true -DperformRelease=true clean deploy`
4. **Trigger Central Publisher Portal**: Sends POST request to the Manual Upload API
5. **Create Git tag**: Creates a Git tag for the release version
6. **Bump version to next SNAPSHOT**: Changes the version to the next SNAPSHOT version
7. **Create GitHub Release**: Creates a GitHub release with release notes
8. **Maven Central verification**: Checks if artifacts are available on Maven Central

### Release Timeline

- **OSSRH Staging**: Artifacts are immediately uploaded to OSSRH
- **Maven Central Sync**: Can take up to 2 hours
- **Search.maven.org**: Updates within 4 hours

## Troubleshooting

### Common Issues

1. **GPG Signing Error**: Ensure the GPG key and passphrase are correct
2. **OSSRH Authentication**: Check username and password
3. **Version already exists**: Make sure the release version doesn't already exist

### Checking Logs

All workflow logs are available in GitHub Actions:
- Go to `Actions` > Workflow Run
- Click on the failed job
- Check the logs for details

### Maven Central Portal

After deployment, you can check the status in the Central Publisher Portal:
- URL: https://central.sonatype.com/
- Login with your OSSRH credentials
- Check the deployment status

## Manual Verification

After a successful release, you can verify:

```bash
# Maven Central available?
curl -I "https://repo1.maven.org/maven2/org/jmdns/jmdns/3.6.3/jmdns-3.6.3.pom"

# Search.maven.org
# https://search.maven.org/artifact/org.jmdns/jmdns/3.6.3/jar
```

## Rollback

If a release fails:

1. **Delete Git tag** (if created):
   ```bash
   git tag -d v3.6.3
   git push origin :refs/tags/v3.6.3
   ```

2. **Reset version**:
   ```bash
   mvn versions:set -DnewVersion=3.6.3-SNAPSHOT
   mvn versions:commit
   git add pom.xml
   git commit -m "Revert to SNAPSHOT after failed release"
   git push origin
   ```

3. **Central Publisher Portal** (if artifacts were uploaded):
   - Login to https://central.sonatype.com/
   - Go to "Publishing" > "Deployments"
   - Find the deployment for your namespace and release version
   - If the deployment is still in progress, you may be able to cancel it
   - For published releases, contact Sonatype support for assistance with removal
