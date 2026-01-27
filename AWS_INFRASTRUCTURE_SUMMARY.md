# AfyaQuest AWS Infrastructure Summary

**Date Created**: 2026-01-27
**AWS Account ID**: 556683673972
**Region**: us-east-1 (US East - N. Virginia)

---

## ✅ Infrastructure Status

### Completed Services

| Service | Resource Name | Status | Resource ID/ARN |
|---------|---------------|--------|-----------------|
| **DynamoDB** | AfyaQuestData | ✅ Active | Table with GSI1 |
| **S3** | afyaquest-media | ✅ Active | Versioning enabled, CORS configured |
| **CloudFront** | AfyaQuest Media CDN | ✅ Deploying | EY6EGE9ERUDKI |
| **IAM Role** | AfyaQuest-Lambda-Role | ✅ Active | arn:aws:iam::556683673972:role/AfyaQuest-Lambda-Role |
| **API Gateway** | AfyaQuest-API | ✅ Active | qsk3ji709f |
| **Lambda Functions** | 5 functions created | ✅ Ready to deploy | See Lambda section below |

### Pending Services

| Service | Action Required |
|---------|-----------------|
| **Cognito** | Provide User Pool ID and Client ID from your setup |
| **Bedrock** | Request model access (see instructions below) |
| **EventBridge** | Rules to be created after Lambda deployment |
| **CloudWatch** | Alarms to be configured |

---

## 📦 DynamoDB Configuration

**Table Name**: `AfyaQuestData`

**Primary Keys**:
- Partition Key (PK): String
- Sort Key (SK): String

**Global Secondary Index**:
- Index Name: GSI1
- GSI1PK: String (Partition Key)
- GSI1SK: String (Sort Key)

**Settings**:
- Billing Mode: PAY_PER_REQUEST (On-Demand)
- Encryption: KMS (Server-side encryption enabled)
- Point-in-Time Recovery: Enabled

**Access Patterns**:
```
USER#<userId>           | PROFILE                       → User profile
USER#<userId>           | ACHIEVEMENT#<id>              → User achievements
USER#<userId>           | PROGRESS#<lessonId>           → Learning progress
USER#<userId>           | QUIZ#<videoId>#<timestamp>    → Quiz results
USER#<userId>           | REPORT#<date>                 → Daily reports
USER#<userId>           | CLIENT#<clientId>             → Client houses
USER#<userId>           | CHAT#<timestamp>              → Chat history
QUESTION#<date>         | Q#<questionId>                → Daily questions
VIDEO#<category>        | VIDEO#<videoId>               → Videos
LESSON#<category>       | LESSON#<lessonId>             → Lessons
FACILITY#<region>       | FACILITY#<facilityId>         → Health facilities
```

---

## 🗂️ S3 Bucket Configuration

**Bucket Name**: `afyaquest-media`

**Folder Structure**:
```
afyaquest-media/
├── videos/              # Video modules
├── thumbnails/          # Video thumbnails
├── profile-pictures/    # User profile pictures
└── lesson-content/      # Lesson PDFs and resources
```

**Settings**:
- Versioning: Enabled
- Encryption: AES256 (Server-side)
- CORS: Configured for GET/HEAD requests
- Public Access: Blocked (access via CloudFront)

**CORS Configuration**:
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

---

## 🌐 CloudFront Distribution

**Distribution ID**: EY6EGE9ERUDKI
**Domain Name**: **d1ghxtad04gj5x.cloudfront.net**
**Status**: InProgress (takes 15-20 minutes to deploy)

**Configuration**:
- Origin: afyaquest-media.s3.us-east-1.amazonaws.com
- Price Class: PriceClass_100 (US, Canada, Europe)
- Protocol: HTTPS redirect enabled
- Compression: Enabled
- Default TTL: 86400 seconds (24 hours)
- Max TTL: 31536000 seconds (1 year)

**Usage in Android App**:
```kotlin
// Example video URL
val videoUrl = "https://d1ghxtad04gj5x.cloudfront.net/videos/lesson1.mp4"
```

---

## 🔐 IAM Role

**Role Name**: AfyaQuest-Lambda-Role
**ARN**: arn:aws:iam::556683673972:role/AfyaQuest-Lambda-Role

**Attached Managed Policies**:
1. AWSLambdaBasicExecutionRole (CloudWatch Logs)
2. AmazonDynamoDBFullAccess (DynamoDB read/write)
3. AmazonCognitoPowerUser (Cognito user management)
4. AmazonS3ReadOnlyAccess (S3 read access)

**Inline Policies**:
- **BedrockAccess**: Allows invoking Bedrock models
  ```json
  {
    "Effect": "Allow",
    "Action": ["bedrock:InvokeModel"],
    "Resource": "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-*"
  }
  ```

---

## 🚀 API Gateway

**API Name**: AfyaQuest-API
**API ID**: qsk3ji709f
**Endpoint Type**: Regional
**Base URL**: `https://qsk3ji709f.execute-api.us-east-1.amazonaws.com`

**Stages** (to be created):
- `dev` → https://qsk3ji709f.execute-api.us-east-1.amazonaws.com/dev
- `prod` → https://qsk3ji709f.execute-api.us-east-1.amazonaws.com/prod

**Planned Endpoints**:

| Method | Path | Lambda Function | Auth |
|--------|------|-----------------|------|
| POST | /auth/register | auth-register | No |
| POST | /auth/login | auth-login | No |
| GET | /auth/me | auth-getCurrentUser | Yes |
| PUT | /auth/change-password | auth-changePassword | Yes |
| GET | /questions/daily | questions-getDaily | Yes |
| POST | /chat/message | chat-sendMessage | Yes |
| GET | /chat/history | chat-getHistory | Yes |
| GET | /videos | videos-getAll | Yes |
| GET | /lessons | lessons-getAll | Yes |
| GET | /lessons/:id | lessons-getById | Yes |
| POST | /progress/lesson | progress-updateLesson | Yes |
| POST | /progress/quiz | progress-submitQuiz | Yes |
| GET | /progress | progress-getUser | Yes |
| POST | /reports | reports-create | Yes |
| GET | /reports | reports-getAll | Yes |
| GET | /reports/:id | reports-getById | Yes |
| PUT | /reports/:id | reports-update | Yes |
| DELETE | /reports/:id | reports-delete | Yes |
| GET | /facilities | map-getFacilities | Yes |
| GET | /clients | map-getClients | Yes |
| PUT | /clients/:id | map-updateClientStatus | Yes |

---

## 🔧 Lambda Functions

### Implemented Functions (5/28)

#### 1. auth-register
**Path**: `lambda-functions/auth/register/`
**Purpose**: Create new user in Cognito and DynamoDB
**Trigger**: API Gateway POST /auth/register
**Environment Variables**:
- DYNAMODB_TABLE
- COGNITO_CLIENT_ID
- AWS_REGION

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "name": "John Doe",
  "phone": "+254712345678",
  "role": "cha"
}
```

**Response**:
```json
{
  "message": "User registered successfully",
  "userId": "uuid",
  "userConfirmed": false,
  "codeDeliveryDetails": { ... }
}
```

---

#### 2. auth-login
**Path**: `lambda-functions/auth/login/`
**Purpose**: Authenticate user and return JWT tokens
**Trigger**: API Gateway POST /auth/login

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response**:
```json
{
  "accessToken": "...",
  "idToken": "...",
  "refreshToken": "...",
  "expiresIn": 3600,
  "user": { ... }
}
```

---

#### 3. auth-getCurrentUser
**Path**: `lambda-functions/auth/getCurrentUser/`
**Purpose**: Get current user profile from DynamoDB
**Trigger**: API Gateway GET /auth/me
**Authorization**: Cognito JWT required

**Response**:
```json
{
  "id": "uuid",
  "email": "user@example.com",
  "name": "John Doe",
  "level": 5,
  "totalPoints": 1250,
  "rank": "Silver CHA",
  "currentStreak": 7
}
```

---

#### 4. chat-sendMessage
**Path**: `lambda-functions/chat/sendMessage/`
**Purpose**: Send message to AI assistant (Bedrock Claude)
**Trigger**: API Gateway POST /chat/message
**Authorization**: Cognito JWT required

**Request Body**:
```json
{
  "message": "What are the symptoms of malaria?"
}
```

**Response**:
```json
{
  "response": "Malaria symptoms include...",
  "messageId": "uuid",
  "timestamp": "2026-01-27T12:00:00Z"
}
```

**Bedrock Configuration**:
- Model: anthropic.claude-3-5-sonnet-20241022-v2:0
- Max Tokens: 500
- System Prompt: Health assistant for CHAs in East Africa

---

#### 5. questions-getDaily
**Path**: `lambda-functions/questions/getDaily/`
**Purpose**: Get 3 daily questions for quiz
**Trigger**: API Gateway GET /questions/daily
**Authorization**: Cognito JWT required

**Response**:
```json
{
  "date": "2026-01-27",
  "questions": [
    {
      "id": "q1-2026-01-27",
      "question": "What is the recommended frequency for handwashing?",
      "options": ["Once per shift", "Before and after patient contact", "Only when dirty", "Every 2 hours"],
      "correctAnswer": "Before and after patient contact",
      "correctAnswerIndex": 1,
      "explanation": "Healthcare workers should wash hands...",
      "category": "hygiene",
      "points": 30,
      "difficulty": "beginner"
    }
  ]
}
```

---

### Deployment

**Prerequisites**:
1. Set Cognito Client ID:
   ```bash
   export COGNITO_CLIENT_ID="your-client-id-here"
   ```

2. Deploy all functions:
   ```bash
   cd /Users/steve/Documents/GitHub/AfyaQuestMobile/lambda-functions
   ./deploy.sh
   ```

3. Or deploy individually:
   ```bash
   cd auth/register
   npm install --production
   zip -r function.zip .
   aws lambda create-function \
     --function-name auth-register \
     --runtime nodejs20.x \
     --role arn:aws:iam::556683673972:role/AfyaQuest-Lambda-Role \
     --handler index.handler \
     --zip-file fileb://function.zip \
     --timeout 30 \
     --memory-size 512 \
     --environment Variables={DYNAMODB_TABLE=AfyaQuestData,COGNITO_CLIENT_ID=your-id,AWS_REGION=us-east-1}
   ```

---

## 🧠 Amazon Bedrock

**Model**: anthropic.claude-3-5-sonnet-20241022-v2:0
**Status**: ⚠️ Access needs to be requested

### Request Bedrock Access

1. Go to AWS Bedrock Console: https://console.aws.amazon.com/bedrock/
2. Click "Model access" in the left sidebar
3. Click "Manage model access"
4. Select:
   - ✅ Anthropic Claude 3.5 Sonnet
   - ✅ Anthropic Claude 3 Sonnet (backup)
5. Click "Request model access"
6. Wait for approval (usually instant)

**Verify Access**:
```bash
aws bedrock list-foundation-models \
  --region us-east-1 \
  --query 'modelSummaries[?contains(modelId, `claude`)].modelId'
```

---

## 🔑 Cognito User Pool

**Status**: ⚠️ Awaiting your configuration

Based on your setup, please provide:
1. **User Pool ID**: (e.g., us-east-1_XXXXXXXXX)
2. **App Client ID**: (e.g., xxxxxxxxxxxxxxxxxxxx)

These will be used in:
- Lambda functions (for authentication)
- Android app (for login/signup)
- API Gateway authorizer

---

## ⚙️ Environment Variables

### For Lambda Functions
Add to each Lambda:
```bash
DYNAMODB_TABLE=AfyaQuestData
COGNITO_CLIENT_ID=<your-client-id>
COGNITO_USER_POOL_ID=<your-pool-id>
S3_BUCKET=afyaquest-media
CLOUDFRONT_DOMAIN=d1ghxtad04gj5x.cloudfront.net
BEDROCK_MODEL_ID=anthropic.claude-3-5-sonnet-20241022-v2:0
AWS_REGION=us-east-1
NODE_ENV=production
```

### For Android App
Add to `AfyaQuest/local.properties`:
```properties
aws.region=us-east-1
aws.cognito.userPoolId=<your-pool-id>
aws.cognito.clientId=<your-client-id>
api.gateway.baseUrl=https://qsk3ji709f.execute-api.us-east-1.amazonaws.com/prod
cloudfront.domain=https://d1ghxtad04gj5x.cloudfront.net
google.maps.apiKey=<your-maps-api-key>
```

---

## 📊 Monitoring & Logging

### CloudWatch Log Groups
Lambda functions automatically create log groups:
- `/aws/lambda/auth-register`
- `/aws/lambda/auth-login`
- `/aws/lambda/auth-getCurrentUser`
- `/aws/lambda/chat-sendMessage`
- `/aws/lambda/questions-getDaily`

**View logs**:
```bash
aws logs tail /aws/lambda/auth-register --follow
```

### Metrics to Monitor
- Lambda invocations and errors
- DynamoDB read/write capacity
- API Gateway 4xx/5xx errors
- CloudFront cache hit ratio
- Bedrock API calls and latency

---

## 💰 Cost Estimate

**Monthly Costs** (500 active users):

| Service | Usage | Cost |
|---------|-------|------|
| API Gateway | ~50k requests | $3.50 |
| Lambda | ~50k invocations, 512MB | $15-20 |
| DynamoDB | On-demand, ~100k reads/writes | $10-15 |
| S3 | 10GB storage, 5k requests | $1 |
| CloudFront | 50GB transfer | $4-8 |
| Bedrock | ~5k API calls | $15-30 |
| CloudWatch | Logs and metrics | $5 |
| **Total** | | **$53-82/month** |

**Cost Optimization Tips**:
1. Enable CloudFront caching to reduce S3 requests
2. Set CloudWatch log retention to 7 days
3. Use Lambda Provisioned Concurrency only if needed
4. Monitor Bedrock usage closely (most expensive service)

---

## 🔐 Security Checklist

- [x] DynamoDB encryption at rest enabled
- [x] S3 versioning enabled
- [x] S3 public access blocked
- [x] Lambda IAM role with least privilege
- [ ] API Gateway Cognito authorizer (pending setup)
- [ ] CloudWatch alarms configured
- [ ] AWS Budget alerts set up
- [ ] GuardDuty enabled (optional)
- [ ] CloudTrail enabled for audit logs

---

## 🧪 Testing

### Test Lambda Functions Locally
```bash
# Install dependencies
cd lambda-functions/auth/register
npm install

# Test locally
node -e "
const handler = require('./index').handler;
const event = {
  body: JSON.stringify({
    email: 'test@example.com',
    password: 'Test123!',
    name: 'Test User'
  })
};
handler(event).then(console.log);
"
```

### Test via AWS Console
1. Go to Lambda console
2. Select function
3. Click "Test"
4. Create test event
5. View results

### Test via CLI
```bash
aws lambda invoke \
  --function-name auth-register \
  --payload '{"body":"{\"email\":\"test@example.com\",\"password\":\"Test123!\",\"name\":\"Test User\"}"}' \
  response.json
cat response.json
```

---

## 📝 Next Steps

### Immediate Actions
1. **Request Bedrock Access**
   - Go to Bedrock console
   - Request Claude 3.5 Sonnet access

2. **Provide Cognito Details**
   - Copy your User Pool ID
   - Copy your App Client ID
   - Add to environment variables

3. **Deploy Lambda Functions**
   ```bash
   export COGNITO_CLIENT_ID="your-client-id"
   cd lambda-functions
   ./deploy.sh
   ```

4. **Set Up API Gateway**
   - Create resources and methods
   - Create Cognito authorizer
   - Link methods to Lambda functions
   - Deploy to prod stage

5. **Test End-to-End**
   - Register test user
   - Login and get tokens
   - Call protected endpoints
   - Test chat with Bedrock

### Future Enhancements
- [ ] Implement remaining 23 Lambda functions
- [ ] Set up EventBridge for scheduled tasks
- [ ] Configure CloudWatch alarms
- [ ] Set up AWS Budgets
- [ ] Add API Gateway caching
- [ ] Implement rate limiting
- [ ] Add X-Ray tracing
- [ ] Set up CI/CD pipeline

---

## 📚 Resources

- **DynamoDB**: https://console.aws.amazon.com/dynamodbv2/home?region=us-east-1#tables
- **S3**: https://s3.console.aws.amazon.com/s3/buckets/afyaquest-media
- **CloudFront**: https://console.aws.amazon.com/cloudfront/v3/home?region=us-east-1#/distributions/EY6EGE9ERUDKI
- **Lambda**: https://console.aws.amazon.com/lambda/home?region=us-east-1
- **API Gateway**: https://console.aws.amazon.com/apigateway/home?region=us-east-1#/apis/qsk3ji709f
- **IAM**: https://console.aws.amazon.com/iam/home#/roles/AfyaQuest-Lambda-Role
- **Bedrock**: https://console.aws.amazon.com/bedrock/home?region=us-east-1#/modelaccess
- **CloudWatch**: https://console.aws.amazon.com/cloudwatch/home?region=us-east-1

---

**Document Version**: 1.0
**Last Updated**: 2026-01-27
**Status**: Infrastructure Ready, Lambda Functions Created, Awaiting Cognito Config
