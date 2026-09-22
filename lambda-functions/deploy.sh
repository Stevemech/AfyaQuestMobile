#!/bin/bash
# AfyaQuest Lambda Deployment
#
# Target: account 556683673972 (stevemech2020@gmail.com), us-east-1
# All functions are prefixed "afyaquest-" to keep this project's namespace
# separate from MedPull and recovery-copilot, which share this account.
# The execution role is scoped to AfyaQuest resources only.

set -e

export AWS_PROFILE="${AWS_PROFILE:-default}"

REGION="us-east-1"
ACCOUNT_ID="556683673972"
ROLE_ARN="arn:aws:iam::${ACCOUNT_ID}:role/AfyaQuest-Lambda-Role"
PREFIX="afyaquest"
RUNTIME="nodejs20.x"
TIMEOUT=30
MEMORY=512

# NOTE: AWS_REGION is a reserved Lambda key and must NOT be set here --
# the runtime provides it automatically. Setting it makes the API reject
# the whole call, which is why the previous version of this script failed.
TABLE_NAME="AfyaQuestData"
COGNITO_CLIENT_ID="${COGNITO_CLIENT_ID:-3qkth9j9bcvvhl9b6q7ml7qih}"
COGNITO_USER_POOL_ID="${COGNITO_USER_POOL_ID:-us-east-1_MxKriIgiC}"
S3_BUCKET="${S3_BUCKET:-afyaquest-media}"
# Groq powers the chat assistant. GROQ_API_KEY is never committed -- export it
# in your shell before deploying, or the chat function deploys keyless and
# returns a 503 until it is set.
GROQ_MODEL="${GROQ_MODEL:-openai/gpt-oss-20b}"
GROQ_API_KEY="${GROQ_API_KEY:-}"
VERIFY_URL="${VERIFY_URL:-https://tkytlmt856.execute-api.us-east-1.amazonaws.com/prod/auth/verify}"

ENV_VARS="Variables={DYNAMODB_TABLE=$TABLE_NAME,COGNITO_CLIENT_ID=$COGNITO_CLIENT_ID,COGNITO_USER_POOL_ID=$COGNITO_USER_POOL_ID,S3_BUCKET=$S3_BUCKET,GROQ_MODEL=$GROQ_MODEL,GROQ_API_KEY=$GROQ_API_KEY,VERIFY_URL=$VERIFY_URL,NODE_ENV=production}"

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ONLY="${1:-}"   # optional: deploy a single dir, e.g. ./deploy.sh auth/login

deploy_lambda() {
    local path=$1
    local name="${PREFIX}-$(echo "$path" | tr '/' '-')"
    local full="$SCRIPT_DIR/$path"

    [ -d "$full" ] || { echo "  missing dir: $path"; return 1; }
    cd "$full"

    if [ -f package.json ] && [ ! -d node_modules ]; then
        npm install --omit=dev --silent >/dev/null 2>&1 || true
    fi

    # Admin functions share an authorization helper. It lives in _shared/ as the
    # single source of truth and is copied in just for packaging.
    local copied_shared=""
    if grep -q "require('./authz')" index.js 2>/dev/null; then
        cp "$SCRIPT_DIR/_shared/authz.js" ./authz.js
        copied_shared="authz.js"
    fi

    rm -f function.zip
    zip -q -r function.zip . -x "*.git*" "*.DS_Store" "function.zip"
    [ -n "$copied_shared" ] && rm -f "$copied_shared"

    if aws lambda get-function --function-name "$name" --region "$REGION" >/dev/null 2>&1; then
        aws lambda update-function-code --function-name "$name" \
            --zip-file fileb://function.zip --region "$REGION" --no-cli-pager >/dev/null
        aws lambda wait function-updated --function-name "$name" --region "$REGION"
        aws lambda update-function-configuration --function-name "$name" \
            --timeout "$TIMEOUT" --memory-size "$MEMORY" --environment "$ENV_VARS" \
            --region "$REGION" --no-cli-pager >/dev/null
        echo "  updated  $name"
    else
        aws lambda create-function --function-name "$name" \
            --runtime "$RUNTIME" --role "$ROLE_ARN" --handler index.handler \
            --zip-file fileb://function.zip --timeout "$TIMEOUT" --memory-size "$MEMORY" \
            --environment "$ENV_VARS" --region "$REGION" \
            --tags Project=AfyaQuest --no-cli-pager >/dev/null
        echo "  created  $name"
    fi

    rm -f function.zip
    cd "$SCRIPT_DIR"
}

echo "Deploying AfyaQuest Lambdas -> $REGION (account $ACCOUNT_ID), prefix '${PREFIX}-'"

if [ -n "$ONLY" ]; then
    deploy_lambda "$ONLY"
else
    find "$SCRIPT_DIR" -name index.js -not -path "*/node_modules/*" -not -path "*/_shared/*" \
      | sed "s|^$SCRIPT_DIR/||; s|/index.js$||" | sort | while IFS= read -r d; do
        deploy_lambda "$d" || echo "  FAILED   $d"
    done
fi

echo "Done."
