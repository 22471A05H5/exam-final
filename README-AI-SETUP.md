# AI Question Generation Setup

## API Key Configuration

### Option 1: OpenAI GPT (Recommended)
1. Get API key from: https://platform.openai.com/api-keys
2. Set environment variable: `OPENAI_API_KEY=your-actual-api-key`
3. Or update `application.properties`: `openai.api.key=your-actual-api-key`

### Option 2: Google Gemini
1. Get API key from: https://makersuite.google.com/app/apikey
2. Set environment variable: `GEMINI_API_KEY=your-actual-api-key`
3. Or update `application.properties`: `gemini.api.key=your-actual-api-key`

## Switch AI Provider
In `application.properties`, change:
```
ai.provider=openai    # for OpenAI GPT
ai.provider=gemini    # for Google Gemini
```

## How It Works
- Faculty creates AI exam → System calls real AI API
- AI generates unique questions based on topic and difficulty
- Questions automatically saved to database
- Students can immediately take the exam

## Fallback
If API fails, system will show error message. No placeholder questions will be created.
