@import ua.edu.ukma.oss.web.support.Labels
@import ua.edu.ukma.oss.web.support.PageContext
@import ua.edu.ukma.oss.web.view.CalendarView
@param PageContext ctx
@param CalendarView cal
@param String navPath
@param String meetingPath
@param String view = "calendar"
<div class="calendar">
    <div class="cal-head">
        <a class="btn secondary small" href="${navPath}?month=${cal.previous().toString()}&view=${view}" aria-label="Попередній місяць">←</a>
        <h3>${ctx.fmt().monthTitle(cal.month())}</h3>
        <a class="btn secondary small" href="${navPath}?month=${cal.next().toString()}&view=${view}" aria-label="Наступний місяць">→</a>
    </div>
    <div class="tabs view-switch" role="tablist" aria-label="Вигляд розкладу">
        <a href="${navPath}?month=${cal.month().toString()}&view=calendar" class="${view.equals("list") ? "" : "active"}">Календар</a>
        <a href="${navPath}?month=${cal.month().toString()}&view=list" class="${view.equals("list") ? "active" : ""}">Список</a>
    </div>
    @if(view.equals("list"))
        <ul class="list meeting-list">
            @for(var week : cal.weeks())
                @for(var day : week)
                    @if(day.inMonth())
                        @for(var m : day.meetings())
                            <li class="row between">
                                <div>
                                    <a href="${meetingPath}${m.id()}"><strong>${m.title()}</strong></a>
                                    <div class="muted small">${ctx.fmt().dateTime(m.scheduledAt())}</div>
                                </div>
                                @template.components.chip(css = Labels.meetingCss(m.status()), text = Labels.meetingStatus(m.status()))
                            </li>
                        @endfor
                    @endif
                @endfor
            @endfor
        </ul>
        @if(cal.weeks().stream().flatMap(w -> w.stream()).noneMatch(d -> d.inMonth() && !d.meetings().isEmpty()))
            <div class="empty">У цьому місяці засідань немає.</div>
        @endif
    @else
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
    @endif
</div>
