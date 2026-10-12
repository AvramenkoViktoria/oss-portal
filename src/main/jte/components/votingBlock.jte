@import ua.edu.ukma.oss.web.support.Labels
@import ua.edu.ukma.oss.web.view.VotingView
@param VotingView v
<div class="voting">
    <div class="row between">
        <h3 style="margin:0">${v.voting().question()}</h3>
        @if(v.voting().isRecorded())
            <span class="verdict ${v.voting().result().name()}">${Labels.voteResult(v.voting().result())}</span>
        @else
            <span class="chip draft">Голоси не внесено</span>
        @endif
    </div>
    @if(v.agendaItemTitle() != null)
        <p class="muted small" style="margin:.2rem 0 0">Пункт порядку денного: ${v.agendaItemTitle()}</p>
    @endif
    @if(v.voting().isRecorded())
        <div class="bar" role="img" aria-label="Підсумок голосування">
            @for(var s : v.segments())
                @if(s.count() > 0)
                    <span class="c-${s.choice().name()}" style="width:${s.percent()}%" title="${Labels.choice(s.choice())}: ${s.count()}"></span>
                @endif
            @endfor
        </div>
        <div class="legend">
            @for(var s : v.segments())
                <span><i class="c-${s.choice().name()}"></i>${Labels.choice(s.choice())}: <strong>${s.count()}</strong></span>
            @endfor
            <span class="muted">Склад: ${v.bodySize()}</span>
        </div>
        <div class="seats" style="grid-template-columns: repeat(${v.grid().columns()}, minmax(0, 1fr))">
            @for(var cell : v.grid().cells())
                <div class="seat c-${cell.choice().name()}" title="${v.seatNames().getOrDefault(cell.membershipId(), "—")} — ${Labels.choice(cell.choice())}">${Labels.initials(v.seatNames().get(cell.membershipId()))}</div>
            @endfor
        </div>
        <details>
            <summary class="small muted" style="cursor:pointer">Поіменний список</summary>
            <table class="data">
                <tbody>
                @for(var cell : v.grid().cells())
                    <tr>
                        <td>${v.seatNames().getOrDefault(cell.membershipId(), "—")}</td>
                        <td><span class="legend"><span><i class="c-${cell.choice().name()}"></i>${Labels.choice(cell.choice())}</span></span></td>
                    </tr>
                @endfor
                </tbody>
            </table>
        </details>
    @endif
</div>
