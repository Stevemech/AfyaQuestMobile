# Afya Quest - Deployment Guide

## Prerequisites

### Development Environment
- Android Studio Hedgehog (2023.1.1) or later
- JDK 11 or later
- Gradle 8.1 or later
- Android SDK 27-36
- Git

### AWS Account
- AWS Account with appropriate permissions
- AWS CLI configured
- Access to:
  - AWS Cognito
  - AWS Lambda
  - API Gateway
  - DynamoDB
  - S3
  - CloudFront
  - Amazon Bedrock (Claude model access)

### Google Services
- Google Maps API Key
- Google Play Console account (for deployment)

---

## Local Development Setup

### 1. Clone Repository
```bash
git clone https://github.com/yourusername/AfyaQuestMobile.git
cd AfyaQuestMobile/AfyaQuest
```

### 2. Configure Local Properties

Create `local.properties` file:
```properties
sdk.dir=/path/to/Android/Sdk

# AWS Configuration
aws.region=us-east-1
aws.cognito.userPoolId=YOUR_USER_POOL_ID
aws.cognito.clientId=YOUR_CLIENT_ID
aws.cognito.clientSecret=YOUR_CLIENT_SECRET
api.gateway.baseUrl=https://YOUR_API_ID.execute-api.us-east-1.amazonaws.com/prod

# Google Maps
google.maps.apiKey=YOUR_GOOGLE_MAPS_API_KEY
```

### 3. Sync Gradle
```bash
./gradlew clean build
```

### 4. Run Tests
```bash
# Unit tests
./gradlew test

# Instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest
```

### 5. Run App
```bash
./gradlew installDebug
```

---

## AWS Backend Deployment

### 1. Deploy Cognito User Pool

```bash
# Create user pool
aws cognito-idp create-user-pool \
  --pool-name AfyaQuestUsers \
  --policies '{
    "PasswordPolicy": {
      "MinimumLength": 6,
      "RequireUppercase": false,
      "RequireLowercase": false,
      "RequireNumbers": false,
      "RequireSymbols": false
    }
  }' \
  --auto-verified-attributes email \
  --schema '[
    {
      "Name": "email",
      "AttributeDataType": "String",
      "Required": true,
      "Mutable": true
    },
    {
      "Name": "name",
      "AttributeDataType": "String",
      "Required": true,
      "Mutable": true
    }
  ]'

# Create app client
aws cognito-idp create-user-pool-client \
  --user-pool-id YOUR_USER_POOL_ID \
  --client-name AfyaQuestMobileApp \
  --no-generate-secret \
  --explicit-auth-flows ALLOW_USER_PASSWORD_AUTH ALLOW_REFRESH_TOKEN_AUTH
```

### 2. Deploy DynamoDB Tables

```bash
# Create main data table
aws dynamodb create-table \
  --table-name AfyaQuestData \
  --attribute-definitions \
    AttributeName=PK,AttributeType=S \
    AttributeName=SK,AttributeType=S \
    AttributeName=GSI1PK,AttributeType=S \
    AttributeName=GSI1SK,AttributeType=S \
  --key-schema \
    AttributeName=PK,KeyType=HASH \
    AttributeName=SK,KeyType=RANGE \
  --global-secondary-indexes '[
    {
      "IndexName": "GSI1",
      "KeySchema": [
        {"AttributeName": "GSI1PK", "KeyType": "HASH"},
        {"AttributeName": "GSI1SK", "KeyType": "RANGE"}
      ],
      "Projection": {"ProjectionType": "ALL"},
      "ProvisionedThroughput": {
        "ReadCapacityUnits": 5,
        "WriteCapacityUnits": 5
      }
    }
  ]' \
  --billing-mode PAY_PER_REQUEST
```

### 3. Deploy Lambda Functions

Navigate to `lambda-functions` directory and deploy each function:

```bash
cd lambda-functions

# Deploy authentication functions
cd auth/login
zip -r function.zip .
aws lambda create-function \
  --function-name afyaquest-auth-login \
  --runtime nodejs20.x \
  --role arn:aws:iam::ACCOUNT_ID:role/lambda-execution-role \
  --handler index.handler \
  --zip-file fileb://function.zip \
  --timeout 30 \
  --memory-size 256 \
  --environment Variables={
    DYNAMODB_TABLE=AfyaQuestData,
    COGNITO_USER_POOL_ID=YOUR_USER_POOL_ID,
    COGNITO_CLIENT_ID=YOUR_CLIENT_ID
  }

# Repeat for all Lambda functions:
# - auth/register
# - auth/getCurrentUser
# - users/getAll
# - lessons/getAll
# - videos/getAll
# - questions/getDaily
# - progress/getUser
# - progress/updateLesson
# - progress/submitQuiz
# - reports/create
# - reports/getAll
# - chat/sendMessage
# - map/getFacilities
# - map/getClients
```

### 4. Deploy API Gateway

```bash
# Create REST API
aws apigateway create-rest-api \
  --name AfyaQuest-API \
  --description "Afya Quest Mobile Backend API" \
  --endpoint-configuration types=REGIONAL

# Create resources and methods
# (Use AWS Console or CloudFormation for easier setup)

# Deploy to stage
aws apigateway create-deployment \
  --rest-api-id YOUR_API_ID \
  --stage-name prod
```

### 5. Configure S3 and CloudFront

```bash
# Create S3 bucket
aws s3 mb s3://afyaquest-media

# Configure bucket for CloudFront
aws s3api put-bucket-cors \
  --bucket afyaquest-media \
  --cors-configuration file://cors-config.json

# Create CloudFront distribution
aws cloudfront create-distribution \
  --origin-domain-name afyaquest-media.s3.amazonaws.com \
  --default-root-object index.html
```

### 6. Request Bedrock Model Access

```bash
# Request access to Claude 3.5 Sonnet in AWS Console
# Bedrock > Model access > Request model access

# Test Bedrock access
aws bedrock-runtime invoke-model \
  --model-id anthropic.claude-3-5-sonnet-20241022-v2:0 \
  --body '{"anthropic_version":"bedrock-2023-05-31","max_tokens":100,"messages":[{"role":"user","content":"Hello"}]}' \
  --cli-binary-format raw-in-base64-out \
  output.json
```

---

## Release Build Configuration

### 1. Generate Signing Key

```bash
keytool -genkey -v \
  -keystore afyaquest-release-key.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias afyaquest
```

**Store securely:**
- Keystore file: `afyaquest-release-key.jks`
- Keystore password
- Key alias: `afyaquest`
- Key password

### 2. Configure Signing in gradle.properties

```properties
AFYAQUEST_RELEASE_STORE_FILE=../afyaquest-release-key.jks
AFYAQUEST_RELEASE_STORE_PASSWORD=your_store_password
AFYAQUEST_RELEASE_KEY_ALIAS=afyaquest
AFYAQUEST_RELEASE_KEY_PASSWORD=your_key_password
```

### 3. Update build.gradle.kts

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(project.properties["AFYAQUEST_RELEASE_STORE_FILE"] as String)
            storePassword = project.properties["AFYAQUEST_RELEASE_STORE_PASSWORD"] as String
            keyAlias = project.properties["AFYAQUEST_RELEASE_KEY_ALIAS"] as String
            keyPassword = project.properties["AFYAQUEST_RELEASE_KEY_PASSWORD"] as String
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### 4. Build Release APK

```bash
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
```

### 5. Build App Bundle (for Play Store)

```bash
./gradlew bundleRelease

# Output: app/build/outputs/bundle/release/app-release.aab
```

---

## Google Play Store Deployment

### 1. Prepare Store Listing

**Required Assets:**
- App icon (512x512 PNG)
- Feature graphic (1024x500 PNG)
- Phone screenshots (2-8 images)
- 7-inch tablet screenshots (optional)
- 10-inch tablet screenshots (optional)
- Short description (80 characters)
- Full description (4000 characters)
- Privacy policy URL

**App Information:**
- App name: Afya Quest
- Category: Health & Fitness
- Content rating: Everyone
- Contact email
- Privacy policy

### 2. Create App in Play Console

1. Go to Google Play Console
2. Create Application
3. Fill out Store Listing
4. Upload Graphics
5. Set Content Rating
6. Set Pricing & Distribution

### 3. Upload App Bundle

1. Go to Release Management > App Releases
2. Create Release (Internal/Alpha/Beta/Production)
3. Upload `app-release.aab`
4. Set version name and release notes
5. Review and rollout

### 4. Submit for Review

- Complete all setup tasks
- Submit for review
- Monitor review status
- Respond to any feedback

---

## Monitoring & Maintenance

### 1. Set Up Crash Reporting

Add Firebase Crashlytics:

```kotlin
// build.gradle.kts
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")
}
```

### 2. Monitor AWS Resources

```bash
# CloudWatch logs for Lambda
aws logs tail /aws/lambda/afyaquest-auth-login --follow

# DynamoDB metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/DynamoDB \
  --metric-name ConsumedReadCapacityUnits \
  --dimensions Name=TableName,Value=AfyaQuestData \
  --start-time 2024-01-27T00:00:00Z \
  --end-time 2024-01-27T23:59:59Z \
  --period 3600 \
  --statistics Sum
```

### 3. Set Up AWS Budgets

```bash
aws budgets create-budget \
  --account-id ACCOUNT_ID \
  --budget file://budget.json
```

### 4. Regular Maintenance Tasks

**Weekly:**
- Check error logs
- Monitor API response times
- Review user feedback

**Monthly:**
- Review AWS costs
- Update dependencies
- Security patches
- Performance optimization

---

## Rollback Procedure

### Android App Rollback

1. Go to Play Console > Release Management
2. Select previous version
3. Promote to production
4. Confirm rollback

### Backend Rollback

```bash
# Rollback Lambda function
aws lambda update-function-code \
  --function-name afyaquest-auth-login \
  --s3-bucket your-lambda-bucket \
  --s3-key previous-version.zip

# Rollback API Gateway deployment
aws apigateway update-stage \
  --rest-api-id YOUR_API_ID \
  --stage-name prod \
  --patch-operations op=replace,path=/deploymentId,value=PREVIOUS_DEPLOYMENT_ID
```

---

## Troubleshooting

### Build Issues

**Problem:** Gradle sync fails
**Solution:**
```bash
./gradlew clean
./gradlew --refresh-dependencies
```

**Problem:** ProGuard errors
**Solution:** Check `proguard-rules.pro` for missing keep rules

### Runtime Issues

**Problem:** Cognito authentication fails
**Solution:** Verify user pool ID and client ID in local.properties

**Problem:** API calls fail with 403
**Solution:** Check API Gateway authorizer configuration

**Problem:** Sync not working
**Solution:**
- Check WorkManager logs
- Verify network connectivity
- Check DynamoDB permissions

### Performance Issues

**Problem:** App feels slow
**Solution:**
- Profile with Android Profiler
- Optimize database queries
- Reduce image sizes
- Enable R8 shrinking

---

## Security Checklist

- [ ] All API keys stored securely (not in source control)
- [ ] ProGuard enabled for release builds
- [ ] Certificate pinning implemented (optional)
- [ ] Encrypted SharedPreferences for sensitive data
- [ ] HTTPS only for all network calls
- [ ] Input validation on all forms
- [ ] SQL injection prevention (Room handles this)
- [ ] XSS prevention in web views (if any)
- [ ] Regular dependency updates
- [ ] Penetration testing completed

---

## Post-Launch Checklist

- [ ] Monitor crash reports daily
- [ ] Respond to user reviews
- [ ] Track key metrics (DAU, retention, engagement)
- [ ] Gather user feedback
- [ ] Plan feature updates
- [ ] Schedule regular maintenance
- [ ] Document known issues
- [ ] Update marketing materials

---

## Support Contacts

**Technical Support:**
- AWS Support: https://console.aws.amazon.com/support/
- Google Play Support: https://support.google.com/googleplay/android-developer/

**Community:**
- GitHub Issues: https://github.com/yourusername/AfyaQuestMobile/issues
- Email: support@afyaquest.com

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2024-01-27 | Initial release |

---

**Deployment Complete!** 🚀

The Afya Quest mobile app is now ready for production deployment to Google Play Store with full AWS serverless backend integration.
