# Copilot CLI BYOK using OpenAI
# Run this in the terminal session where you want to use Copilot CLI.
# This script also sets optional Atlassian session credentials for Jira and Confluence.

# Editable non-secret defaults
$defaultCopilotProviderType = "openai"
$defaultCopilotProviderBaseUrl = "https://api.openai.com/v1"
$defaultCopilotModel = "gpt-5.4"
$defaultAtlassianBaseUrl = "https://millses.atlassian.net"
$defaultAtlassianEmail = "millslf@gmail.com"
$defaultJiraProjectKey = "FBC"
$defaultJiraBoardId = "35"
$defaultJiraBoardUrl = "https://millses.atlassian.net/jira/software/projects/FBC/boards/35"
$defaultConfluenceSpaceKey = "FB"
$defaultConfluenceSpaceUrl = "https://millses.atlassian.net/wiki/spaces/FB/overview"
$defaultConfluenceHomepageId = "4587688"

function Read-PlainSecret {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Prompt
    )

    $secureValue = Read-Host $Prompt -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureValue)

    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    }
    finally {
        if ($bstr -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
        }
    }
}

function Set-SessionValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    Set-Item -Path "Env:$Name" -Value $Value
}

$env:COPILOT_PROVIDER_TYPE = $defaultCopilotProviderType
$env:COPILOT_PROVIDER_BASE_URL = $defaultCopilotProviderBaseUrl
$env:COPILOT_MODEL = $defaultCopilotModel

$plainKey = Read-PlainSecret -Prompt "Enter your OpenAI API key"
Set-SessionValue -Name "COPILOT_PROVIDER_API_KEY" -Value $plainKey

Write-Host "Copilot CLI BYOK environment configured for this terminal session."
Write-Host "Provider: $env:COPILOT_PROVIDER_TYPE"
Write-Host "Model: $env:COPILOT_MODEL"

$configureAtlassian = Read-Host "Configure Jira/Confluence credentials for this session? (y/N)"

if ($configureAtlassian -match '^(y|yes)$') {
    $atlassianBaseUrl = Read-Host "Enter your Atlassian base URL [$defaultAtlassianBaseUrl]"
    if ([string]::IsNullOrWhiteSpace($atlassianBaseUrl)) {
        $atlassianBaseUrl = $defaultAtlassianBaseUrl
    }
    $atlassianEmail = Read-Host "Enter your Atlassian email [$defaultAtlassianEmail]"
    if ([string]::IsNullOrWhiteSpace($atlassianEmail)) {
        $atlassianEmail = $defaultAtlassianEmail
    }
    $atlassianApiToken = Read-PlainSecret -Prompt "Enter your Atlassian API token"

    Set-SessionValue -Name "ATLASSIAN_BASE_URL" -Value $atlassianBaseUrl
    Set-SessionValue -Name "ATLASSIAN_EMAIL" -Value $atlassianEmail
    Set-SessionValue -Name "ATLASSIAN_API_TOKEN" -Value $atlassianApiToken

    # Explicit service aliases for tools or scripts that expect separate names.
    Set-SessionValue -Name "JIRA_BASE_URL" -Value $atlassianBaseUrl
    Set-SessionValue -Name "JIRA_EMAIL" -Value $atlassianEmail
    Set-SessionValue -Name "JIRA_API_TOKEN" -Value $atlassianApiToken
    Set-SessionValue -Name "JIRA_PROJECT_KEY" -Value $defaultJiraProjectKey
    Set-SessionValue -Name "JIRA_BOARD_ID" -Value $defaultJiraBoardId
    Set-SessionValue -Name "JIRA_BOARD_URL" -Value $defaultJiraBoardUrl
    Set-SessionValue -Name "CONFLUENCE_BASE_URL" -Value $atlassianBaseUrl
    Set-SessionValue -Name "CONFLUENCE_EMAIL" -Value $atlassianEmail
    Set-SessionValue -Name "CONFLUENCE_API_TOKEN" -Value $atlassianApiToken
    Set-SessionValue -Name "CONFLUENCE_SPACE_KEY" -Value $defaultConfluenceSpaceKey
    Set-SessionValue -Name "CONFLUENCE_SPACE_URL" -Value $defaultConfluenceSpaceUrl
    Set-SessionValue -Name "CONFLUENCE_HOMEPAGE_ID" -Value $defaultConfluenceHomepageId

    Write-Host "Atlassian environment configured for this terminal session."
    Write-Host "Base URL: $env:ATLASSIAN_BASE_URL"
    Write-Host "Email: $env:ATLASSIAN_EMAIL"
    Write-Host "Jira project: $env:JIRA_PROJECT_KEY"
    Write-Host "Jira board: $env:JIRA_BOARD_ID"
    Write-Host "Jira board URL: $env:JIRA_BOARD_URL"
    Write-Host "Confluence space: $env:CONFLUENCE_SPACE_KEY"
    Write-Host "Confluence space URL: $env:CONFLUENCE_SPACE_URL"
    Write-Host "Jira and Confluence aliases were also set."
}
else {
    Write-Host "Skipping Jira/Confluence session setup."
}

# Optional: show provider help
copilot help providers
