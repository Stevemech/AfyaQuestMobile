# Afya Quest - AWS Infrastructure Setup Guide

## Phase 1: AWS Infrastructure Setup

This guide walks through setting up the complete AWS infrastructure for the Afya Quest mobile application.

---

## 1. Prerequisites

- AWS Account with administrative access
- AWS CLI installed and configured
- Node.js 20.x installed (for Lambda development)
- Basic understanding of AWS services

---

## 2. AWS Cognito User Pool Setup

### 2.1 Create User Pool

1. Navigate to AWS Cognito Console
2. Click "Create user pool"
3. Configure sign-in options:
   - **Sign-in options**: Email
   - **Password policy**: Minimum 6 characters
   - **MFA**: Optional (recommended for production)

4. Configure sign-up experience:
   - Enable self-registration
   - Required attributes: name, email, phone_number
   - Custom attributes:
     - `custom:level` (Number)
     - `custom:totalPoints` (Number)
     - `custom:rank` (String)
     - `custom:currentStreak` (Number)
     - `custom:role` (String) - Values: cha, supervisor, admin

5. Configure message delivery:
   - Email provider: Cognito default (or use SES for production)
   - Verification: Email verification required

6. Integrate your app:
   - User pool name: `AfyaQuestUsers`
   - App client name: `AfyaQuestMobileApp`
   - App type: Public client
   - Authentication flows: ALLOW_USER_PASSWORD_AUTH, ALLOW_REFRESH_TOKEN_AUTH

7. Save User Pool ID and App Client ID for later use

### 2.2 Configure User Pool

```bash
# Export environment variables
export USER_POOL_ID="us-east-1_XXXXXXXXX"
export CLIENT_ID="xxxxxxxxxxxxxxxxxxxx"
export AWS_REGION="us-east-1"
```

---

## 3. DynamoDB Table Setup

### 3.1 Create Table

```bash
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
  --billing-mode PAY_PER_REQUEST \
  --global-secondary-indexes \
    "IndexName=GSI1,KeySchema=[{AttributeName=GSI1PK,KeyType=HASH},{AttributeName=GSI1SK,KeyType=RANGE}],Projection={ProjectionType=ALL}" \
  --region us-east-1
```

### 3.2 Enable Encryption at Rest

```bash
aws dynamodb update-table \
  --table-name AfyaQuestData \
  --sse-specification Enabled=true,SSEType=KMS \
  --region us-east-1
```

### 3.3 Table Schema Reference

| Access Pattern | PK | SK | GSI1PK | GSI1SK |
|----------------|----|----|--------|--------|
| Get user profile | USER#\<userId\> | PROFILE | - | - |
| Get user achievements | USER#\<userId\> | ACHIEVEMENT#\<id\> | - | - |
| Get user progress | USER#\<userId\> | PROGRESS#\<lessonId\> | - | - |
| Get user quiz results | USER#\<userId\> | QUIZ#\<videoId\>#\<timestamp\> | - | - |
| Get user reports | USER#\<userId\> | REPORT#\<date\> | - | - |
| Get user clients | USER#\<userId\> | CLIENT#\<clientId\> | - | - |
| Get user chat history | USER#\<userId\> | CHAT#\<timestamp\> | - | - |
| Get daily questions | QUESTION#\<date\> | Q#\<questionId\> | - | - |
| Get videos by category | VIDEO#\<category\> | VIDEO#\<videoId\> | - | - |
| Get lessons by category | LESSON#\<category\> | LESSON#\<lessonId\> | - | - |
| Get facilities by region | FACILITY#\<region\> | FACILITY#\<facilityId\> | - | - |
| Query by date range | - | - | DATE#\<date\> | \<timestamp\> |

---

## 4. S3 Bucket Setup

### 4.1 Create S3 Bucket

```bash
aws s3 mb s3://afyaquest-media --region us-east-1

# Create folder structure
aws s3api put-object --bucket afyaquest-media --key videos/
aws s3api put-object --bucket afyaquest-media --key thumbnails/
aws s3api put-object --bucket afyaquest-media --key profile-pictures/
aws s3api put-object --bucket afyaquest-media --key lesson-content/
```

### 4.2 Configure CORS

Create `cors-config.json`:
```json
{
  "CORSRules": [
    {
      "AllowedOrigins": ["*"],
      "AllowedMethods": ["GET", "HEAD"],
      "AllowedHeaders": ["*"],
      "MaxAgeSeconds": 3000
    }
  ]
}
```

Apply CORS:
```bash
aws s3api put-bucket-cors --bucket afyaquest-media --cors-configuration file://cors-config.json
```

### 4.3 Enable Versioning

```bash
aws s3api put-bucket-versioning \
  --bucket afyaquest-media \
  --versioning-configuration Status=Enabled
```

### 4.4 Configure Lifecycle Policy (Optional)

Create `lifecycle-policy.json`:
```json
{
  "Rules": [
    {
      "Id": "DeleteOldVersions",
      "Status": "Enabled",
      "NoncurrentVersionExpiration": {
        "NoncurrentDays": 90
      }
    }
  ]
}
```

Apply policy:
```bash
aws s3api put-bucket-lifecycle-configuration \
  --bucket afyaquest-media \
  --lifecycle-configuration file://lifecycle-policy.json
```

---

## 5. CloudFront Distribution Setup

### 5.1 Create Distribution

```bash
aws cloudfront create-distribution \
  --origin-domain-name afyaquest-media.s3.us-east-1.amazonaws.com \
  --default-root-object index.html
```

### 5.2 Configuration

- **Price Class**: Use Only U.S., Canada and Europe (PriceClass_100) for cost savings
- **Caching**:
  - Videos: 24 hours (86400 seconds)
  - Thumbnails: 7 days
  - Other content: 1 hour
- **Compression**: Enabled
- **HTTP/2**: Enabled
- **SSL Certificate**: Use default CloudFront certificate or custom ACM certificate

### 5.3 Save CloudFront Domain

```bash
# Example: d1234567890abc.cloudfront.net
export CLOUDFRONT_DOMAIN="<your-distribution-domain>"
```

---

## 6. API Gateway Setup

### 6.1 Create REST API

```bash
aws apigateway create-rest-api \
  --name AfyaQuest-API \
  --description "AfyaQuest Mobile API" \
  --region us-east-1
```

### 6.2 Create Cognito Authorizer

```bash
aws apigateway create-authorizer \
  --rest-api-id <api-id> \
  --name CognitoAuthorizer \
  --type COGNITO_USER_POOLS \
  --provider-arns arn:aws:cognito-idp:us-east-1:<account-id>:userpool/${USER_POOL_ID} \
  --identity-source method.request.header.Authorization
```

### 6.3 Configure CORS

Enable CORS for all methods:
- Access-Control-Allow-Origin: *
- Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
- Access-Control-Allow-Headers: Content-Type, Authorization

### 6.4 Configure Rate Limiting

```bash
# Create usage plan
aws apigateway create-usage-plan \
  --name AfyaQuest-UsagePlan \
  --throttle rateLimit=1000,burstLimit=2000 \
  --quota limit=100000,period=DAY
```

### 6.5 Create Stages

```bash
# Deploy to dev stage
aws apigateway create-deployment \
  --rest-api-id <api-id> \
  --stage-name dev

# Deploy to prod stage
aws apigateway create-deployment \
  --rest-api-id <api-id> \
  --stage-name prod
```

---

## 7. IAM Roles Setup

### 7.1 Lambda Execution Role

Create `lambda-trust-policy.json`:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

Create role:
```bash
aws iam create-role \
  --role-name AfyaQuest-Lambda-Role \
  --assume-role-policy-document file://lambda-trust-policy.json
```

### 7.2 Attach Policies

```bash
# CloudWatch Logs
aws iam attach-role-policy \
  --role-name AfyaQuest-Lambda-Role \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

# DynamoDB Access
aws iam attach-role-policy \
  --role-name AfyaQuest-Lambda-Role \
  --policy-arn arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess

# Cognito Access
aws iam attach-role-policy \
  --role-name AfyaQuest-Lambda-Role \
  --policy-arn arn:aws:iam::aws:policy/AmazonCognitoPowerUser

# S3 Access
aws iam attach-role-policy \
  --role-name AfyaQuest-Lambda-Role \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess

# Bedrock Access
aws iam put-role-policy \
  --role-name AfyaQuest-Lambda-Role \
  --policy-name BedrockAccess \
  --policy-document '{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": [
          "bedrock:InvokeModel"
        ],
        "Resource": "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-3-5-sonnet-*"
      }
    ]
  }'
```

---

## 8. Amazon Bedrock Setup

### 8.1 Request Model Access

1. Navigate to Amazon Bedrock Console
2. Click "Model access" in the left sidebar
3. Click "Manage model access"
4. Select models:
   - Anthropic Claude 3.5 Sonnet (recommended)
   - Meta Llama 3.2 (alternative)
5. Click "Request model access"
6. Wait for approval (usually instant for Claude)

### 8.2 Verify Access

```bash
aws bedrock list-foundation-models --region us-east-1 --query 'modelSummaries[?contains(modelId, `claude`)].modelId'
```

### 8.3 Configure Model

```bash
export BEDROCK_MODEL_ID="anthropic.claude-3-5-sonnet-20241022-v2:0"
export BEDROCK_REGION="us-east-1"
```

---

## 9. Lambda Functions Structure

### 9.1 Create Base Directory

```bash
mkdir -p lambda-functions/{auth,users,lessons,videos,questions,progress,reports,chat,map,scheduled}
```

### 9.2 Function List (28 Lambda Functions)

**Authentication (5)**:
- `auth-register` - Create new user in Cognito + DynamoDB
- `auth-login` - Authenticate user and return JWT tokens
- `auth-getCurrentUser` - Fetch user details from DynamoDB
- `auth-changePassword` - Update user password in Cognito
- `auth-logout` - Client-side token invalidation

**Users (3)**:
- `users-getAll` - List all users (admin only)
- `users-getById` - Get user by ID
- `users-update` - Update user profile

**Learning (4)**:
- `lessons-getAll` - Get all lessons with category filter
- `lessons-getById` - Get lesson details
- `videos-getAll` - Get all videos with CloudFront URLs
- `questions-getDaily` - Get 3 daily questions based on date

**Progress (4)**:
- `progress-getUser` - Get user's learning progress
- `progress-updateLesson` - Mark lesson as completed
- `progress-submitQuiz` - Submit quiz results
- `progress-updateStreak` - Update daily streak

**Reports (5)**:
- `reports-create` - Create daily report
- `reports-getAll` - Get all reports for user
- `reports-getById` - Get report by ID
- `reports-update` - Update report
- `reports-delete` - Delete report

**Chat (2)**:
- `chat-sendMessage` - Send message to Bedrock and get response
- `chat-getHistory` - Retrieve chat history

**Map (3)**:
- `map-getFacilities` - Get health facilities by region
- `map-getClients` - Get client houses for user
- `map-updateClientStatus` - Update client visit status

**Scheduled (2)**:
- `scheduled-resetDaily` - Reset daily tasks (EventBridge: 00:00 UTC)
- `scheduled-updateLeaderboard` - Update leaderboard (EventBridge: hourly)

### 9.3 Lambda Environment Variables Template

Create `.env.lambda`:
```bash
DYNAMODB_TABLE=AfyaQuestData
COGNITO_USER_POOL_ID=<pool-id>
COGNITO_CLIENT_ID=<client-id>
S3_BUCKET=afyaquest-media
CLOUDFRONT_DOMAIN=<distribution-url>
BEDROCK_MODEL_ID=anthropic.claude-3-5-sonnet-20241022-v2:0
AWS_REGION=us-east-1
NODE_ENV=production
```

---

## 10. EventBridge Rules for Scheduled Functions

### 10.1 Daily Reset (00:00 UTC)

```bash
aws events put-rule \
  --name AfyaQuest-DailyReset \
  --schedule-expression "cron(0 0 * * ? *)" \
  --state ENABLED

aws events put-targets \
  --rule AfyaQuest-DailyReset \
  --targets "Id"="1","Arn"="arn:aws:lambda:us-east-1:<account-id>:function:scheduled-resetDaily"
```

### 10.2 Leaderboard Update (Hourly)

```bash
aws events put-rule \
  --name AfyaQuest-LeaderboardUpdate \
  --schedule-expression "rate(1 hour)" \
  --state ENABLED

aws events put-targets \
  --rule AfyaQuest-LeaderboardUpdate \
  --targets "Id"="1","Arn"="arn:aws:lambda:us-east-1:<account-id>:function:scheduled-updateLeaderboard"
```

---

## 11. CloudWatch Setup

### 11.1 Create Log Groups

```bash
# Lambda function log groups are created automatically
# Create custom metric filters and alarms

# Example: High error rate alarm
aws cloudwatch put-metric-alarm \
  --alarm-name AfyaQuest-HighErrorRate \
  --alarm-description "Alert when error rate exceeds 5%" \
  --metric-name Errors \
  --namespace AWS/Lambda \
  --statistic Average \
  --period 300 \
  --threshold 0.05 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2
```

### 11.2 Cost Monitoring

```bash
# Enable billing alerts
aws ce create-cost-category-definition \
  --name AfyaQuest \
  --rules file://cost-rules.json
```

---

## 12. AWS Budgets Setup

```bash
aws budgets create-budget \
  --account-id <account-id> \
  --budget '{
    "BudgetName": "AfyaQuest-Monthly",
    "BudgetLimit": {
      "Amount": "100",
      "Unit": "USD"
    },
    "BudgetType": "COST",
    "TimeUnit": "MONTHLY"
  }' \
  --notifications-with-subscribers '[
    {
      "Notification": {
        "NotificationType": "ACTUAL",
        "ComparisonOperator": "GREATER_THAN",
        "Threshold": 80,
        "ThresholdType": "PERCENTAGE"
      },
      "Subscribers": [
        {
          "SubscriptionType": "EMAIL",
          "Address": "your-email@example.com"
        }
      ]
    }
  ]'
```

---

## 13. Configuration Summary

After completing the setup, save these values to your Android project's `local.properties`:

```properties
# AWS Configuration
aws.region=us-east-1
aws.cognito.userPoolId=us-east-1_XXXXXXXXX
aws.cognito.clientId=xxxxxxxxxxxxxxxxxxxx
api.gateway.baseUrl=https://<api-id>.execute-api.us-east-1.amazonaws.com/prod

# Optional: Google Maps API Key (for map features)
google.maps.apiKey=<your-maps-api-key>
```

---

## 14. Testing Infrastructure

### 14.1 Test Cognito

```bash
# Sign up test user
aws cognito-idp sign-up \
  --client-id ${CLIENT_ID} \
  --username test@afyaquest.com \
  --password TestPass123! \
  --user-attributes Name=name,Value="Test User" Name=email,Value=test@afyaquest.com
```

### 14.2 Test DynamoDB

```bash
# Put test item
aws dynamodb put-item \
  --table-name AfyaQuestData \
  --item '{
    "PK": {"S": "USER#test123"},
    "SK": {"S": "PROFILE"},
    "name": {"S": "Test User"},
    "email": {"S": "test@afyaquest.com"}
  }'

# Get item
aws dynamodb get-item \
  --table-name AfyaQuestData \
  --key '{
    "PK": {"S": "USER#test123"},
    "SK": {"S": "PROFILE"}
  }'
```

### 14.3 Test S3

```bash
# Upload test file
echo "Test content" > test.txt
aws s3 cp test.txt s3://afyaquest-media/videos/test.txt

# Verify via CloudFront
curl https://${CLOUDFRONT_DOMAIN}/videos/test.txt
```

---

## 15. Security Checklist

- [ ] Enable DynamoDB encryption at rest
- [ ] Enable S3 bucket versioning
- [ ] Configure S3 bucket to block public access
- [ ] Use CloudFront signed URLs for private content (optional)
- [ ] Enable API Gateway request validation
- [ ] Set up API rate limiting
- [ ] Configure IAM least-privilege policies
- [ ] Enable CloudTrail for audit logging
- [ ] Enable GuardDuty for threat detection (optional)
- [ ] Use AWS Secrets Manager for sensitive config (optional)
- [ ] Enable MFA for Cognito (production)

---

## 16. Cost Optimization Tips

1. **Use DynamoDB On-Demand**: Pay only for what you use
2. **CloudFront PriceClass_100**: Limit to North America and Europe
3. **Lambda Reserved Concurrency**: Set limits to prevent runaway costs
4. **S3 Intelligent-Tiering**: Move infrequently accessed objects to cheaper storage
5. **CloudWatch Log Retention**: Set to 7-14 days
6. **API Gateway Caching**: Enable to reduce Lambda invocations

---

## 17. Monitoring Dashboard

Create a CloudWatch dashboard to monitor:
- Lambda invocation count and errors
- DynamoDB read/write capacity
- API Gateway latency and 4xx/5xx errors
- S3 bucket size and request count
- Cognito user pool metrics
- Monthly cost estimates

---

## Next Steps

After completing this setup:

1. Verify all resources are created successfully
2. Save all IDs and ARNs to your configuration files
3. Proceed to Phase 2: Android Project Setup
4. Begin implementing Lambda functions in Phase 3

---

## Troubleshooting

**Issue**: Cognito user pool creation fails
**Solution**: Ensure you have the correct permissions and are in the correct region

**Issue**: DynamoDB table creation fails
**Solution**: Check if a table with the same name already exists

**Issue**: Lambda cannot access DynamoDB
**Solution**: Verify IAM role has correct policies attached

**Issue**: API Gateway returns 403 Forbidden
**Solution**: Check Cognito authorizer configuration and JWT token format

**Issue**: Bedrock model access denied
**Solution**: Request model access in Bedrock console and wait for approval

---

## Support Resources

- AWS Documentation: https://docs.aws.amazon.com/
- Cognito Developer Guide: https://docs.aws.amazon.com/cognito/
- DynamoDB Developer Guide: https://docs.aws.amazon.com/dynamodb/
- API Gateway Developer Guide: https://docs.aws.amazon.com/apigateway/
- Bedrock User Guide: https://docs.aws.amazon.com/bedrock/
- AWS SDK for JavaScript: https://docs.aws.amazon.com/sdk-for-javascript/

---

**Document Version**: 1.0
**Last Updated**: 2026-01-26
**Author**: Claude Code Assistant
