@import ua.edu.ukma.oss.web.support.Labels
@import ua.edu.ukma.oss.web.support.PageContext
@import ua.edu.ukma.oss.web.view.CalendarView
@param PageContext ctx
@param CalendarView cal
@param String navPath
@param String meetingPath
<div class="calendar">
    <div class="cal-head">
        <a class="btn secondary small" href="${navPath}?month=${cal.previous().toString()}" aria-label="Попередній місяць">←</a>
        <h3>${ctx.fmt().monthTitle(cal.month())}</h3>
        <a class="btn secondary small" href="${navPath}?month=${cal.next().toString()}" aria-label="Наступний місяць">→</a>
    </div>
    <div class="cal-scroll">
        <div class="cal-grid">
            @for(var name : new String[] {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Нд"})
                <div class="cal-dow">${name}</div>
            @endfor
            @for(var week : cal.weeks())
                @for(var day : week)
                    <div class="cal-day ${day.inMonth() ? "" : "out"} ${day.today() ? "today" : ""}">
                        <div class="cal-num">${day.date().getDayOfMonth()}</div>
                        @for(var m : day.meetings())
                            <a class="cal-event ${Labels.meetingCss(m.status())}" href="${meetingPath}${m.id()}" title="${m.title()}">
                                <span class="cal-time">${ctx.fmt().time(m.scheduledAt())}</span> ${m.title()}
                            </a>
                        @endfor
                    </div>
                @endfor
            @endfor
        </div>
    </div>
    <div class="legend" style="margin-top:.6rem">
        <span><i style="background:var(--approved)"></i>заплановано</span>
        <span><i style="background:var(--published)"></i>відбулося</span>
        <span><i style="background:var(--danger)"></i>скасовано</span>
    </div>
</div>
