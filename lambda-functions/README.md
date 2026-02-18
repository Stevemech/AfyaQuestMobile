# AfyaQuest Lambda Functions

This directory contains all AWS Lambda functions for the AfyaQuest mobile application backend.

## Structure

```
lambda-functions/
├── auth/
│   ├── register/           # User registration
│   ├── login/              # User authentication
│   ├── getCurrentUser/     # Get user profile
│   └── changePassword/     # Password change (TODO)
├── users/
│   ├── getAll/            # List all users (TODO)
│   ├── getById/           # Get user by ID (TODO)
│   └── update/            # Update user profile (TODO)
├── lessons/
│   ├── getAll/            # Get all lessons (TODO)
│   └── getById/           # Get lesson details (TODO)
├── videos/
│   └── getAll/            # Get all videos (TODO)
├── questions/
│   └── getDaily/          # Get daily questions ✅
├── progress/
│   ├── getUser/           # Get user progress (TODO)
│   ├── updateLesson/      # Update lesson progress (TODO)
│   ├── submitQuiz/        # Submit quiz results (TODO)
│   └── updateStreak/      # Update daily streak (TODO)
├── reports/
│   ├── create/            # Create daily report (TODO)
│   ├── getAll/            # Get all reports (TODO)
│   ├── getById/           # Get report by ID (TODO)
│   ├── update/            # Update report (TODO)
│   └── delete/            # Delete report (TODO)
├── chat/
│   ├── sendMessage/       # Send AI chat message ✅
│   └── getHistory/        # Get chat history (TODO)
├── map/
│   ├── getFacilities/     # Get health facilities (TODO)
│   ├── getClients/        # Get client houses (TODO)
│   └── updateClientStatus/ # Update visit status (TODO)
├── scheduled/
│   ├── resetDaily/        # Daily reset task (TODO)
│   └── updateLeaderboard/ # Leaderboard update (TODO)
└── deploy.sh              # Deployment script ✅
```

## Implemented Functions (5/28)

### ✅ Auth Functions
1. **auth-register** - Creates new user in Cognito and DynamoDB
2. **auth-login** - Authenticates user and returns JWT tokens
3. **auth-getCurrentUser** - Retrieves current user profile

### ✅ Chat Functions
4. **chat-sendMessage** - AI chat with Amazon Bedrock (Claude 3.5 Sonnet)

### ✅ Questions Functions
5. **questions-getDaily** - Returns 3 daily questions (with sample data generator)

## Deployment

### Prerequisites
- AWS CLI configured
- Node.js 20.x installed
- Cognito User Pool created
- DynamoDB table created
- IAM role created

### Environment Variables
Set these before deploying:
```bash
export COGNITO_CLIENT_ID="your-client-id"
export CLOUDFRONT_DOMAIN="your-cloudfront-domain.cloudfront.net"
```

### Deploy All Functions
```bash
cd lambda-functions
./deploy.sh
```

### Deploy Single Function
```bash
cd auth/register
npm install --production
zip -r function.zip .
aws lambda update-function-code \
  --function-name auth-register \
  --zip-file fileb://function.zip \
  --region af-south-1
```

## Testing Functions

### Test auth-register
```bash
aws lambda invoke \
  --function-name auth-register \
  --payload '{
    "body": "{\"email\":\"test@example.com\",\"password\":\"Test123!\",\"name\":\"Test User\",\"phone\":\"+254712345678\"}"
  }' \
  --region af-south-1 \
  response.json
cat response.json
```

### Test auth-login
```bash
aws lambda invoke \
  --function-name auth-login \
  --payload '{
    "body": "{\"email\":\"test@example.com\",\"password\":\"Test123!\"}"
  }' \
  --region af-south-1 \
  response.json
cat response.json
```

### Test questions-getDaily
```bash
aws lambda invoke \
  --function-name questions-getDaily \
  --payload '{}' \
  --region af-south-1 \
  response.json
cat response.json
```

### Test chat-sendMessage
```bash
aws lambda invoke \
  --function-name chat-sendMessage \
  --payload '{
    "body": "{\"message\":\"What are the signs of malaria?\"}",
    "requestContext": {
      "authorizer": {
        "claims": {
          "sub": "test-user-id"
        }
      }
    }
  }' \
  --region af-south-1 \
  response.json
cat response.json
```

## Configuration

### Lambda Environment Variables
Each Lambda function has access to:
- `DYNAMODB_TABLE` - DynamoDB table name
- `COGNITO_CLIENT_ID` - Cognito app client ID
- `S3_BUCKET` - S3 bucket name
- `CLOUDFRONT_DOMAIN` - CloudFront distribution domain
- `BEDROCK_MODEL_ID` - Bedrock model ID
- `AWS_REGION` - AWS region
- `NODE_ENV` - Environment (production)

### IAM Permissions
The Lambda execution role has:
- CloudWatch Logs (write)
- DynamoDB (read/write)
- Cognito (read/write)
- S3 (read)
- Bedrock (invoke model)

## API Gateway Integration

After deploying Lambda functions, integrate with API Gateway:

1. Create resources and methods
2. Set up Cognito authorizer
3. Link methods to Lambda functions
4. Enable CORS
5. Deploy to stages (dev, prod)

Example API endpoints:
- POST /auth/register
- POST /auth/login
- GET /auth/me
- GET /questions/daily
- POST /chat/message

## Monitoring

View Lambda logs:
```bash
aws logs tail /aws/lambda/auth-register --follow
```

View all function metrics:
```bash
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Invocations \
  --dimensions Name=FunctionName,Value=auth-register \
  --start-time 2026-01-27T00:00:00Z \
  --end-time 2026-01-27T23:59:59Z \
  --period 3600 \
  --statistics Sum
```

## Development

### Adding New Function
1. Create directory: `mkdir -p category/functionName`
2. Create `index.js` with handler
3. Create `package.json` with dependencies
4. Add to `deploy.sh` LAMBDAS array
5. Run `./deploy.sh`

### Local Testing
Use AWS SAM for local testing:
```bash
sam local invoke auth-register -e events/register.json
```

## TODO - Remaining Functions (23/28)

### High Priority
- [ ] auth-changePassword
- [ ] progress-submitQuiz
- [ ] progress-updateStreak
- [ ] reports-create
- [ ] reports-getAll
- [ ] chat-getHistory

### Medium Priority
- [ ] videos-getAll
- [ ] lessons-getAll
- [ ] lessons-getById
- [ ] progress-getUser
- [ ] progress-updateLesson
- [ ] reports-getById
- [ ] reports-update
- [ ] reports-delete

### Low Priority
- [ ] users-getAll
- [ ] users-getById
- [ ] users-update
- [ ] map-getFacilities
- [ ] map-getClients
- [ ] map-updateClientStatus
- [ ] scheduled-resetDaily
- [ ] scheduled-updateLeaderboard

## Cost Optimization

Current configuration:
- **Memory**: 512 MB
- **Timeout**: 30 seconds
- **Provisioned Concurrency**: None

Expected monthly costs (500 active users):
- Lambda invocations: ~$15-20
- Bedrock API calls: ~$15-30
- Data transfer: ~$5

## Security

- All functions use environment variables for secrets
- Cognito authorizer validates JWT tokens
- DynamoDB encryption at rest enabled
- CloudWatch logs for audit trail
- No hardcoded credentials

## Support

For issues or questions:
1. Check CloudWatch logs
2. Review IAM permissions
3. Verify environment variables
4. Test with AWS Console Lambda test feature

## Version History

- **v1.0.0** (2026-01-27) - Initial implementation
  - Auth functions (register, login, getCurrentUser)
  - Chat with Bedrock
  - Daily questions
  - Deployment script
