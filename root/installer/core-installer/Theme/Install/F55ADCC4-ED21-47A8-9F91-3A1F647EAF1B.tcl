proc CreateWindow.F55ADCC4-ED21-47A8-9F91-3A1F647EAF1B {wizard id} {
    set base  [$wizard widget get $id]
    set frame $base.titleframe

    grid rowconfigure    $base 3 -weight 1
    grid columnconfigure $base 0 -weight 1

    frame $frame -bd 0 -relief flat -background white
    grid  $frame -row 0 -column 0 -sticky nsew

    grid rowconfigure    $frame 1 -weight 1
    grid columnconfigure $frame 0 -weight 1

    Label $frame.title -background white -anchor nw -justify left -autowrap 1  -font TkCaptionFont -textvariable [$wizard variable $id -text1]
    grid $frame.title -row 0 -column 0 -sticky new -padx 5 -pady 5
    $id widget set Title -widget $frame.title

    Label $frame.subtitle -background white -anchor nw -autowrap 1  -justify left -textvariable [$wizard variable $id -text2]
    grid $frame.subtitle -row 1 -column 0 -sticky new -padx [list 20 5]
    $id widget set Subtitle -widget $frame.subtitle

    label $frame.icon -borderwidth 0 -background white -anchor c
    grid  $frame.icon -row 0 -column 1 -rowspan 2
    $id widget set Icon -widget $frame.icon -type image

    Separator $base.separator -relief groove -orient horizontal
    grid $base.separator -row 1 -column 0 -sticky ew 

    Label $base.caption -anchor nw -justify left -autowrap 1  -textvariable [$wizard variable $id -text3]
    grid $base.caption -row 2 -sticky nsew -padx 8 -pady [list 8 4]
    $id widget set Caption -widget $base.caption

    frame $base.clientarea
    grid  $base.clientarea -row 3 -sticky nsew -padx {20
 8} -pady {20 4}

    $id widget set ClientArea -widget $base.clientarea -type frame

    grid rowconfigure $base.clientarea 2 -weight 1
    grid columnconfigure $base.clientarea 0 -weight 1

    ttk::checkbutton $base.clientarea.java -text "Download and install JDK component"  -variable ::info(InstallJDK)
    grid $base.clientarea.java -row 0 -column 0 -sticky nw -pady 4

    ttk::checkbutton $base.clientarea.openoffice -text "Download and install OpenOffice.org component"  -variable ::info(InstallOpenOffice)
    grid $base.clientarea.openoffice -row 1 -column 0 -sticky nw

#    ttk::checkbutton $base.clientarea.64bit -text "Use 64 bit components when available" -variable ::info(Download64BitComponents)
#    grid $base.clientarea.64bit -row 2 -column 0 -sticky nw -pady {20 0}

    Label $base.message -anchor nw -justify left -autowrap 1  -textvariable [$wizard variable $id -text4]
    grid $base.message -row 4 -sticky nsew -padx 8 -pady [list 4 8]
    $id widget set Message -widget $base.message
}

