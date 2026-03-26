import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'

dayjs.extend(utc)
dayjs.extend(timezone)

export function formatRange(start: string | number, end: string | number) {
  return `${dayjs.tz(start, 'Asia/Shanghai').format('YYYY-MM-DD HH:mm')} - ${dayjs.tz(end, 'Asia/Shanghai').format('HH:mm')}`
}

export function isPast(input: string | number) {
  const ms = typeof input === 'number' ? input : dayjs.tz(input, 'Asia/Shanghai').valueOf()
  return dayjs.tz(ms, 'Asia/Shanghai').isBefore(dayjs().tz('Asia/Shanghai'))
}

