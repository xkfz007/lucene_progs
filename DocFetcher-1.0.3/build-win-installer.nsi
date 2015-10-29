; NSIS script for building the windows installer
;
; This must be run after running the Ant script,
; because it expects to find the DocFetcher jar in the build folder.
;
; When building a new release, remember to update the version number in the next command.

!define VERSION 1.0.3

SetCompress force
SetCompressor /SOLID lzma
Name "DocFetcher ${VERSION}"
XPStyle on
OutFile build\docfetcher_${VERSION}_win32_setup.exe
InstallDir $PROGRAMFILES\DocFetcher
Page directory
Page instfiles
UninstPage uninstConfirm
UninstPage instfiles
AutoCloseWindow true

!include "FileFunc.nsh"
!insertmacro GetTime

LoadLanguageFile "${NSISDIR}\Contrib\Language files\Afrikaans.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Albanian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Arabic.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Basque.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Belarusian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Bosnian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Breton.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Bulgarian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Catalan.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Croatian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Czech.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Danish.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Dutch.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\English.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Estonian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Farsi.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Finnish.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\French.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Galician.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\German.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Greek.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Hebrew.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Hungarian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Icelandic.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Indonesian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Irish.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Italian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Japanese.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Korean.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Kurdish.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Latvian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Lithuanian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Luxembourgish.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Macedonian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Malay.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Mongolian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Norwegian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\NorwegianNynorsk.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Polish.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Portuguese.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\PortugueseBR.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Romanian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Russian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Serbian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\SerbianLatin.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\SimpChinese.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Slovak.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Slovenian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Spanish.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\SpanishInternational.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Swedish.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Thai.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\TradChinese.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Turkish.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Ukrainian.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Uzbek.nlf"
LoadLanguageFile "${NSISDIR}\Contrib\Language files\Welsh.nlf"

Function .onInit
	ReadRegStr $R0 HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DocFetcher" "UninstallString"
	StrCmp $R0 "" done
	MessageBox MB_OKCANCEL|MB_ICONEXCLAMATION \
	"DocFetcher is already installed. $\n$\nClick 'OK' to remove the \
	previous version or 'Cancel' to cancel this upgrade." \
	IDOK uninst
	Abort
	
	uninst:
		ClearErrors
		ExecWait '$R0 /S _?=$INSTDIR'
		IfErrors no_remove_uninstaller
			Delete $INSTDIR\uninstaller.exe
			RMDIR $INSTDIR
		no_remove_uninstaller:
	done:
FunctionEnd

Section "DocFetcher"
	; Copy files
    SetOutPath $INSTDIR
    File resources\windows\DocFetcher.exe
    File resources\windows\DocFetcher.bat
    File resources\daemon\docfetcher-daemon-win.exe
    File resources\ChangeLog.txt
    File resources\Readme.txt
    
    SetOutPath $INSTDIR\licenses
	File /r /x .svn resources\licenses\*.*
	
	SetOutPath $INSTDIR\help
    File /r /x .svn resources\help\*.*
    
    SetOutPath $INSTDIR\icons
    File /r /x .svn resources\icons\*.*
    
    SetOutPath $INSTDIR\lang
    File /r /x .svn resources\lang\*.*
    
    SetOutPath $INSTDIR\lib
    File /x *.so /x *.so.* /x swt*linux*.jar /x .svn lib\*.*
    File build\net.sourceforge.docfetcher_*.jar
    
    ; Uninstaller
    WriteUninstaller $INSTDIR\uninstaller.exe
    
    ; Start menu entries
    CreateDirectory $SMPROGRAMS\DocFetcher
    CreateShortCut $SMPROGRAMS\DocFetcher\DocFetcher.lnk $INSTDIR\DocFetcher.exe
    CreateShortCut "$SMPROGRAMS\DocFetcher\Uninstall DocFetcher.lnk" $INSTDIR\uninstaller.exe
    CreateShortCut $SMPROGRAMS\DocFetcher\Readme.lnk $INSTDIR\Readme.txt
    CreateShortCut $SMPROGRAMS\DocFetcher\ChangeLog.lnk $INSTDIR\ChangeLog.txt
    
    ; Write to registry
    Var /GLOBAL regkey
    Var /GLOBAL homepage
    StrCpy $regkey "Software\Microsoft\Windows\CurrentVersion\Uninstall\DocFetcher"
    StrCpy $homepage "http://docfetcher.sourceforge.net"
    WriteRegStr HKLM $regkey "DisplayName" "DocFetcher"
    WriteRegStr HKLM $regkey "UninstallString" "$INSTDIR\uninstaller.exe"
    WriteRegStr HKLM $regkey "InstallLocation" $INSTDIR
    WriteRegStr HKLM $regkey "DisplayIcon" "$INSTDIR\DocFetcher.exe,0"
    WriteRegStr HKLM $regkey "HelpLink" $homepage
    WriteRegStr HKLM $regkey "URLUpdateInfo" $homepage
    WriteRegStr HKLM $regkey "URLInfoAbout" $homepage
    WriteRegStr HKLM $regkey "DisplayVersion" "${VERSION}"
    WriteRegDWORD HKLM $regkey "NoModify" 1
    WriteRegDWORD HKLM $regkey "NoRepair" 1
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Run" "DocFetcher-Daemon" "$INSTDIR\docfetcher-daemon-win.exe"
    
    ; Convert Unix line terminators to Windows line terminators
    Push "$INSTDIR\ChangeLog.txt"
    Call ConvertUnixNewLines
    Push "$INSTDIR\Readme.txt"
    Call ConvertUnixNewLines
    ; we won't fix the license files and hope the user won't look at them with Notepad
    
    ; Replace strings
    ${GetTime} "" "L" $0 $1 $2 $3 $4 $5 $6
    Push "$INSTDIR\ChangeLog.txt"
    Push "@DATE@"
    Push "$2-$1-$0"
    Call ReplaceInFile
    
    Push "$INSTDIR\ChangeLog.txt"
    Push "@VERSION@"
    Push "${VERSION}"
    Call ReplaceInFile
    
    Push "$INSTDIR\Readme.txt"
    Push "@VERSION@"
    Push "${VERSION}"
    Call ReplaceInFile
    
    ; The ReplaceInFile calls can leave "*.old" files behind
    Delete $INSTDIR\*.old
	
	; Register context menu entry
	ExecWait '"$INSTDIR\DocFetcher.exe" --register-contextmenu'
	
	; Launch daemon
	Exec '"$INSTDIR\docfetcher-daemon-win.exe"'
SectionEnd

Section "un.Uninstall"
	; Kill daemon
	Processes::KillProcess "docfetcher-daemon-win"
	Sleep 1000
	
	; Unregister context menu entry
	ExecWait '"$INSTDIR\DocFetcher.exe" --unregister-contextmenu'
	
	; Remove program folder
	Delete $INSTDIR\DocFetcher.exe
	Delete $INSTDIR\uninstaller.exe
	Delete $INSTDIR\DocFetcher.bat
	Delete $INSTDIR\docfetcher-daemon-win.exe
	Delete $INSTDIR\user.properties
	Delete $INSTDIR\ChangeLog.txt
	Delete $INSTDIR\Readme.txt
    Delete $INSTDIR\hs_err_pid*.log
    
	RMDir /r $INSTDIR\help\
    
	Delete $INSTDIR\icons\*.gif
	Delete $INSTDIR\icons\*.png
    RMDir $INSTDIR\icons
    
	Delete $INSTDIR\lang\*.properties
	RMDir $INSTDIR\lang
	
	Delete $INSTDIR\lib\*.jar
	Delete $INSTDIR\lib\*.dll
	Delete $INSTDIR\lib\*.so
    RMDir $INSTDIR\lib
    
    RMDir /r $INSTDIR\indexes
	RMDir /r $INSTDIR\licenses
	RMDir $INSTDIR
    
    ; Remove application data folder
    RMDir /r $APPDATA\DocFetcher
	
	; Remove start menu entries
	Delete $SMPROGRAMS\DocFetcher\DocFetcher.lnk
	Delete "$SMPROGRAMS\DocFetcher\Uninstall DocFetcher.lnk"
	Delete $SMPROGRAMS\DocFetcher\Manual.lnk
	Delete $SMPROGRAMS\DocFetcher\Readme.lnk
	Delete $SMPROGRAMS\DocFetcher\ChangeLog.lnk
	RMDir $SMPROGRAMS\DocFetcher
    
    ; Remove registry key
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\DocFetcher"
    DeleteRegValue HKCU "Software\Microsoft\Windows\CurrentVersion\Run" "DocFetcher-Daemon"
SectionEnd

; Function for converting Unix line terminators to Windows line terminators
Function ConvertUnixNewLines
Exch $R0 ;file #1 path
Push $R1 ;file #1 handle
Push $R2 ;file #2 path
Push $R3 ;file #2 handle
Push $R4 ;data
Push $R5
 
	FileOpen $R1 $R0 r
	GetTempFileName $R2
	FileOpen $R3 $R2 w
 
	loopRead:
		ClearErrors
		FileRead $R1 $R4 
		IfErrors doneRead
 
			StrCpy $R5 $R4 1 -1
			StrCmp $R5 $\n 0 +4
			StrCpy $R5 $R4 1 -2
			StrCmp $R5 $\r +3
			StrCpy $R4 $R4 -1
			StrCpy $R4 "$R4$\r$\n"
 
		FileWrite $R3 $R4
 
	Goto loopRead
	doneRead:
 
	FileClose $R3
	FileClose $R1
 
	SetDetailsPrint none
	Delete $R0
	Rename $R2 $R0
	SetDetailsPrint both
 
Pop $R5
Pop $R4
Pop $R3
Pop $R2
Pop $R1
Pop $R0
FunctionEnd

; Function to replace strings in a file
Function ReplaceInFile
 
  ClearErrors  ; want to be a newborn
 
  Exch $0      ; REPLACEMENT
  Exch
  Exch $1      ; SEARCH_TEXT
  Exch 2
  Exch $2      ; SOURCE_FILE
 
  Push $R0     ; SOURCE_FILE file handle
  Push $R1     ; temporary file handle
  Push $R2     ; unique temporary file name
  Push $R3     ; a line to sar/save
  Push $R4     ; shift puffer
 
  IfFileExists $2 +1 RIF_error      ; knock-knock
  FileOpen $R0 $2 "r"               ; open the door
 
  GetTempFileName $R2               ; who's new?
  FileOpen $R1 $R2 "w"              ; the escape, please!
 
  RIF_loop:                         ; round'n'round we go
    FileRead $R0 $R3                ; read one line
    IfErrors RIF_leaveloop          ; enough is enough
    RIF_sar:                        ; sar - search and replace
      Push "$R3"                    ; (hair)stack
      Push "$1"                     ; needle
      Push "$0"                     ; blood
      Call StrReplace               ; do the bartwalk
      StrCpy $R4 "$R3"              ; remember previous state
      Pop $R3                       ; gimme s.th. back in return!
      StrCmp "$R3" "$R4" +1 RIF_sar ; loop, might change again!
    FileWrite $R1 "$R3"             ; save the newbie
  Goto RIF_loop                     ; gimme more
 
  RIF_leaveloop:                    ; over'n'out, Sir!
    FileClose $R1                   ; S'rry, Ma'am - clos'n now
    FileClose $R0                   ; me 2
 
    Delete "$2.old"                 ; go away, Sire
    Rename "$2" "$2.old"            ; step aside, Ma'am
    Rename "$R2" "$2"               ; hi, baby!
    
    ClearErrors                     ; now i AM a newborn
    Goto RIF_out                    ; out'n'away
 
  RIF_error:                        ; ups - s.th. went wrong...
    SetErrors                       ; ...so cry, boy!
 
  RIF_out:                          ; your wardrobe?
  Pop $R4
  Pop $R3
  Pop $R2
  Pop $R1
  Pop $R0
  Pop $2
  Pop $0
  Pop $1
 
FunctionEnd

; StrReplace
; Replaces all ocurrences of a given needle within a haystack with another string
; Written by dandaman32
 
Var STR_REPLACE_VAR_0
Var STR_REPLACE_VAR_1
Var STR_REPLACE_VAR_2
Var STR_REPLACE_VAR_3
Var STR_REPLACE_VAR_4
Var STR_REPLACE_VAR_5
Var STR_REPLACE_VAR_6
Var STR_REPLACE_VAR_7
Var STR_REPLACE_VAR_8
 
Function StrReplace
  Exch $STR_REPLACE_VAR_2
  Exch 1
  Exch $STR_REPLACE_VAR_1
  Exch 2
  Exch $STR_REPLACE_VAR_0
    StrCpy $STR_REPLACE_VAR_3 -1
    StrLen $STR_REPLACE_VAR_4 $STR_REPLACE_VAR_1
    StrLen $STR_REPLACE_VAR_6 $STR_REPLACE_VAR_0
    loop:
      IntOp $STR_REPLACE_VAR_3 $STR_REPLACE_VAR_3 + 1
      StrCpy $STR_REPLACE_VAR_5 $STR_REPLACE_VAR_0 $STR_REPLACE_VAR_4 $STR_REPLACE_VAR_3
      StrCmp $STR_REPLACE_VAR_5 $STR_REPLACE_VAR_1 found
      StrCmp $STR_REPLACE_VAR_3 $STR_REPLACE_VAR_6 done
      Goto loop
    found:
      StrCpy $STR_REPLACE_VAR_5 $STR_REPLACE_VAR_0 $STR_REPLACE_VAR_3
      IntOp $STR_REPLACE_VAR_8 $STR_REPLACE_VAR_3 + $STR_REPLACE_VAR_4
      StrCpy $STR_REPLACE_VAR_7 $STR_REPLACE_VAR_0 "" $STR_REPLACE_VAR_8
      StrCpy $STR_REPLACE_VAR_0 $STR_REPLACE_VAR_5$STR_REPLACE_VAR_2$STR_REPLACE_VAR_7
      StrLen $STR_REPLACE_VAR_6 $STR_REPLACE_VAR_0
      Goto loop
    done:
  Pop $STR_REPLACE_VAR_1 ; Prevent "invalid opcode" errors and keep the
  Pop $STR_REPLACE_VAR_1 ; stack as it was before the function was called
  Exch $STR_REPLACE_VAR_0
FunctionEnd
 
!macro _strReplaceConstructor OUT NEEDLE NEEDLE2 HAYSTACK
  Push "${HAYSTACK}"
  Push "${NEEDLE}"
  Push "${NEEDLE2}"
  Call StrReplace
  Pop "${OUT}"
!macroend
 
!define StrReplace '!insertmacro "_strReplaceConstructor"'
